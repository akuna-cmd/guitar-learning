package com.example.thetest1.data.remote

import com.example.thetest1.domain.model.AiAssistantRequest

object AiAssistantPromptBuilder {
    private const val RoleInstruction =
        "Ти - високопрофейсійних викладач з гітари, який інтегрований в Android застосунок для навчання гри на гітарі."
    private const val OutputInstruction =
        "На основі поданої теорії/табів дай відповіді на запитання учня. Коротко (максимум 10-15 речень), професійно, нейтрально, без вступних слів, українською мовою."
    private const val MeasureRangeTemplate =
        "УВАГА: Користувач питає КОНКРЕТНО про такти з %1\$d по %2\$d. Аналізуй ТІЛЬКИ ЦІ ТАКТИ і повністю ігноруй всі інші. Формат переданих табів: 'Beat X: [Струна Y [Назва] (лад Z), ...]' де Струна 1 - це найтонша 'e' струна (ВЕРХНЯ лінія на класичних малюнках табів), а Струна 6 - найтовща басова 'E' струна (НИЖНЯ лінія на табах). Ноти, згруповані в одних дужках 'Beat X: [...]', граються ОДНОЧАСНО (в один удар). Відповідай так, ніби інших тактів не існує."

    fun build(request: AiAssistantRequest): String {
        val contextInstruction = request.measureRange?.let { range ->
            MeasureRangeTemplate.format(range.first, range.last)
        }.orEmpty()

        return """
            $RoleInstruction
            $OutputInstruction

            $contextInstruction

            Теорія:
            ${request.theory}

            Таби:
            ${request.tabs}

            Питання:
            ${request.question}
        """.trimIndent()
    }
}
