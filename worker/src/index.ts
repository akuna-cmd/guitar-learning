import { createRemoteJWKSet, jwtVerify } from "jose";

interface Env {
  FIREBASE_PROJECT_ID: string;
  DEFAULT_GEMINI_MODEL: string;
  GEMINI_API_KEY: string;
}

interface GeminiRequest {
  model?: unknown;
  prompt?: unknown;
}

interface GeminiResponse {
  candidates?: Array<{
    content?: {
      parts?: Array<{
        text?: string;
      }>;
    };
  }>;
  error?: {
    message?: string;
  };
}

const firebaseJwks = createRemoteJWKSet(
  new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"),
);

const allowedModels = new Set([
  "gemini-2.5-flash-lite",
  "gemini-2.5-flash",
]);

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json",
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Headers": "Authorization, Content-Type",
      "Access-Control-Allow-Methods": "POST, OPTIONS",
    },
  });
}

function readBearerToken(request: Request): string | null {
  const header = request.headers.get("Authorization")?.trim() ?? "";
  if (!header.startsWith("Bearer ")) return null;
  const token = header.slice("Bearer ".length).trim();
  return token.length > 0 ? token : null;
}

async function verifyFirebaseToken(idToken: string, projectId: string): Promise<string> {
  const { payload } = await jwtVerify(idToken, firebaseJwks, {
    issuer: `https://securetoken.google.com/${projectId}`,
    audience: projectId,
  });
  const userId = typeof payload.sub === "string" ? payload.sub.trim() : "";
  if (!userId) {
    throw new Error("Missing Firebase user id.");
  }
  return userId;
}

function normalizeModel(requestedModel: unknown, fallbackModel: string): string {
  const candidate = typeof requestedModel === "string" ? requestedModel.trim() : "";
  return allowedModels.has(candidate) ? candidate : fallbackModel;
}

function normalizePrompt(prompt: unknown): string {
  return typeof prompt === "string" ? prompt.trim() : "";
}

function extractGeminiText(payload: GeminiResponse): string {
  return payload.candidates
    ?.flatMap((candidate) => candidate.content?.parts ?? [])
    .map((part) => part.text?.trim() ?? "")
    .filter((part) => part.length > 0)
    .join("\n")
    .trim() ?? "";
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (request.method === "OPTIONS") {
      return jsonResponse({}, 204);
    }

    if (request.method !== "POST") {
      return jsonResponse({ error: "Method not allowed." }, 405);
    }

    if (!env.GEMINI_API_KEY?.trim()) {
      return jsonResponse({ error: "GEMINI_API_KEY is not configured." }, 500);
    }

    const idToken = readBearerToken(request);
    if (!idToken) {
      return jsonResponse({ error: "Missing Authorization bearer token." }, 401);
    }

    try {
      await verifyFirebaseToken(idToken, env.FIREBASE_PROJECT_ID);
    } catch {
      return jsonResponse({ error: "Invalid Firebase ID token." }, 401);
    }

    let requestBody: GeminiRequest;
    try {
      requestBody = (await request.json()) as GeminiRequest;
    } catch {
      return jsonResponse({ error: "Invalid JSON body." }, 400);
    }

    const prompt = normalizePrompt(requestBody.prompt);
    if (!prompt) {
      return jsonResponse({ error: "Prompt is required." }, 400);
    }

    const model = normalizeModel(requestBody.model, env.DEFAULT_GEMINI_MODEL);
    const geminiResponse = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "x-goog-api-key": env.GEMINI_API_KEY,
        },
        body: JSON.stringify({
          contents: [
            {
              parts: [
                {
                  text: prompt,
                },
              ],
            },
          ],
        }),
      },
    );

    const payload = (await geminiResponse.json()) as GeminiResponse;
    if (!geminiResponse.ok) {
      return jsonResponse(
        { error: payload.error?.message ?? "Gemini request failed." },
        geminiResponse.status,
      );
    }

    const text = extractGeminiText(payload);
    if (!text) {
      return jsonResponse({ error: "Gemini returned an empty response." }, 502);
    }

    return jsonResponse({
      model,
      text,
    });
  },
};
