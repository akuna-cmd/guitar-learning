const BIOMECH = {
    1:{1:8,2:1,3:2,4:4}, 2:{1:1,2:8,3:1,4:2},
    3:{1:3,2:1,3:8,4:1}, 4:{1:6,2:3,3:1,4:8}
};

const biomechCost = (f, t) => (BIOMECH[f] || {})[t] || 5;
const transCost = (p1, p2, d) => ((p2 - p1) ** 2) / Math.max(d, .125);

function normalizeStringIndex(s) {
    if (typeof s !== 'number') return s;
    if (s < 1 || s > 6) return s;
    return 7 - s;
}

function ensureBody() {
    if (!document.body) {
        const body = document.getElementsByTagName('body')[0] || document.documentElement || document.createElement('body');
        document.body = body;
        if (document.documentElement && !body.parentNode) {
            document.documentElement.appendChild(body);
        }
    }
    if (document.body && !document.body.addEventListener) {
        document.body.addEventListener = function() {};
    }
}

function configCost(frets, pos) {
    const pr = frets.filter(f => f > 0);
    if (!pr.length) return 0;
    let c = 0;
    const span = Math.max(...pr) - Math.min(...pr);
    if (span > 4) c += (span - 4) ** 2 * 3;
    for (const f of pr) {
        const r = f - pos;
        if (r < 0) c += 50;
        else if (r > 4) c += (r - 4) ** 2 * 2;
    }
    return c;
}

function detectBarre(notes) {
    const pr = notes.filter(n => n.fret > 0);
    if (!pr.length) return null;
    const mn = Math.min(...pr.map(n => n.fret));
    const on = pr.filter(n => n.fret === mn);
    if (on.length >= 3) {
        const sortedStrings = on.map(n => n.string).sort((a, b) => a - b);
        const span = sortedStrings[sortedStrings.length - 1] - sortedStrings[0];
        const hasConsecutive = sortedStrings.some((s, i) => i > 0 && (s - sortedStrings[i - 1]) === 1);
        if (span >= 2 && hasConsecutive) return mn;
    }
    return null;
}

function dpFingering(data) {
    if (!data.length) return [];
    const MAX = 22;
    function cands(d) {
        const pr = d.notes.filter(n => n.fret > 0).map(n => n.fret);
        if (!pr.length) return Array.from({ length: MAX }, (_, i) => i + 1);
        const mn = Math.max(1, Math.min(...pr));
        const lo = Math.max(1, mn - 3);
        const hi = Math.min(MAX, mn + 2);
        return Array.from({ length: hi - lo + 1 }, (_, i) => lo + i);
    }
    let dp = new Map();
    for (const pos of cands(data[0])) {
        dp.set(pos, { cost: configCost(data[0].notes.map(n => n.fret), pos), prev: null, pos });
    }
    for (let i = 1; i < data.length; i++) {
        const d = data[i];
        const fr = d.notes.map(n => n.fret);
        const pd = data[i - 1].duration;
        const nd = new Map();
        for (const pos of cands(d)) {
            const cc = configCost(fr, pos);
            let bc = Infinity;
            let bp = null;
            for (const [pp, { cost: pc }] of dp) {
                const tCost = pc + transCost(pp, pos, pd) + cc;
                if (tCost < bc) {
                    bc = tCost;
                    bp = dp.get(pp);
                }
            }
            nd.set(pos, { cost: bc, prev: bp, pos });
        }
        dp = nd;
    }
    let best = null;
    for (const v of dp.values()) if (!best || v.cost < best.cost) best = v;
    if (!best) return [];
    const path = [];
    while (best) {
        path.push(best.pos);
        best = best.prev;
    }
    return path.reverse();
}

function assignLhFingers(notes, pos, barre, prevF, prevBeatPlacement) {
    const sorted = [...notes].sort((a, b) => (a.fret - b.fret) || (a.string - b.string));
    const lhMap = {};
    const newF = {};

    for (const { string: s, fret: f, isDead } of sorted) {
        if (f === 0) {
            lhMap[`${s}_${f}`] = '-';
            continue;
        }
        if (f === barre && !isDead) {
            lhMap[`${s}_${f}`] = `${leftHandName(1)} (${t('possibleBarre', { fret: f })})`;
            newF[s] = 1;
        }
    }

    const toAssign = sorted.filter(n => n.fret > 0 && !n.isDead && !(barre !== null && n.fret === barre));
    if (!toAssign.length) return { lhMap, newF };

    function baseFinger(f) {
        return Math.min(Math.max(f - pos + 1, 1), 4);
    }

    function candSet(n) {
        const b = baseFinger(n.fret);
        const out = [b, b - 1, b + 1, b - 2, b + 2].filter(v => v >= 1 && v <= 4);
        if (prevF[n.string] && !out.includes(prevF[n.string])) out.push(prevF[n.string]);
        return out;
    }

    function pairPenalty(a, b) {
        let p = 0;
        if (a.finger === b.finger) {
            if (a.fret !== b.fret) p += 140;
            else p += (Math.abs(a.string - b.string) <= 1) ? 95 : 55;
        }
        if (a.fret < b.fret && a.finger > b.finger) p += 8;
        if (a.fret > b.fret && a.finger < b.finger) p += 8;
        if (a.fret === b.fret) {
            if (a.string > b.string && a.finger > b.finger) p += 64;
            if (a.string < b.string && a.finger < b.finger) p += 64;
        }
        return p;
    }

    let bestCost = Infinity;
    let bestAssign = null;
    const current = [];

    function dfs(i, cost) {
        if (cost >= bestCost) return;
        if (i >= toAssign.length) {
            bestCost = cost;
            bestAssign = current.slice();
            return;
        }
        const n = toAssign[i];
        const b = baseFinger(n.fret);
        const candidates = candSet(n);
        for (const finger of candidates) {
            let c = cost;
            c += Math.abs(finger - b) * 2.5;
            if (prevF[n.string]) c += Math.abs(finger - prevF[n.string]) * 2.0;
            const prevSameString = prevBeatPlacement?.[n.string];
            if (prevSameString && prevSameString.fret === n.fret) {
                if (prevSameString.finger === finger) c -= 14;
                else c += 120;
            }
            const prevAdjacentLower = prevBeatPlacement?.[n.string + 1];
            const prevAdjacentUpper = prevBeatPlacement?.[n.string - 1];
            if (
                (prevAdjacentLower && prevAdjacentLower.fret === n.fret && prevAdjacentLower.finger === finger) ||
                (prevAdjacentUpper && prevAdjacentUpper.fret === n.fret && prevAdjacentUpper.finger === finger)
            ) {
                c += 10;
            }
            for (const a of current) {
                c += pairPenalty(a, { string: n.string, fret: n.fret, finger });
            }
            current.push({ string: n.string, fret: n.fret, finger });
            dfs(i + 1, c);
            current.pop();
        }
    }

    dfs(0, 0);

    for (const item of (bestAssign || [])) {
        lhMap[`${item.string}_${item.fret}`] = leftHandName(item.finger);
        newF[item.string] = item.finger;
    }

    return { lhMap, newF };
}

class RightHandSM {
    constructor() { this._last = 'm'; }

    getFingers(notes) {
        const res = {};
        const FULL = { p: rightHandName('p'), i: rightHandName('i'), m: rightHandName('m'), a: rightHandName('a') };
        const ordered = [...notes].sort((a, b) => a.string - b.string);
        if (ordered.length === 1) {
            const s = ordered[0].string;
            if (s >= 5) return { [s]: FULL.p };
            const nx = this._last === 'm' ? 'i' : 'm';
            this._last = nx;
            return { [s]: FULL[nx] };
        }
        const topMap = { 1: 'a', 2: 'm', 3: 'i' };
        let thumbUsed = false;
        for (const n of ordered) {
            if (n.string >= 4 && !thumbUsed) {
                res[n.string] = FULL.p;
                thumbUsed = true;
            } else if (n.string <= 3) {
                res[n.string] = FULL[topMap[n.string] || 'i'];
            } else {
                res[n.string] = FULL.i;
            }
        }
        return res;
    }
}

function buildInstructions(notes, rawNotes, lhMap, rhMap, barre, pos, isTapping) {
    const inst = [];
    if (isTapping) {
        inst.push(t('tappingInstruction'));
        return inst;
    }

    const noteCount = notes.length;
    if (barre !== null) {
        inst.push(t('positionBarre', { fret: barre }));
        inst.push(t('barreHelp'));
    } else if (pos > 0) {
        const pr = notes.filter(n => n.fret > 0 && !n.isDead);
        if (pr.length > 0) {
            const span = Math.max(...pr.map(n => n.fret)) - Math.min(...pr.map(n => n.fret));
            inst.push(t('positionAround', { fret: pos }));
            if (span > 3) inst.push(t('wideStretchInstruction'));
        }
    }

    if (noteCount === 1) {
        const { string: s, fret: f } = notes[0];
        const lh = lhMap[`${s}_${f}`] || '?';
        const rh = rhMap[s] || rightHandName('i');
        if (rawNotes[0].isDead) inst.push(t('leftHandMuted', { string: STRING_NAMES[s] }));
        else if (f === 0) inst.push(t('leftHandOpen', { string: STRING_NAMES[s] }));
        else inst.push(t('leftHandFinger', { finger: lh, string: STRING_NAMES[s], fret: f }));
        inst.push(t('rightHandFinger', { finger: rh, direction: s >= 4 ? t('directionDown') : t('directionUp') }));
    } else {
        inst.push(t('chord', { count: noteCount }));
        const lhParts = notes.map(n => n.isDead
            ? `${STRING_SHORT[n.string]}-${t('mutedShort')}`
            : `${STRING_SHORT[n.string]}-${n.fret === 0 ? t('openShort') : n.fret}(${lhMap[`${n.string}_${n.fret}`] || '?'})`
        );
        inst.push(t('leftHandSummary', { items: lhParts.join(', ') }));
        if (notes.some(n => n.string >= 4) && notes.some(n => n.string <= 3)) {
            inst.push(t('rightHandArpeggio'));
        } else {
            const dir = notes[0].string >= 4 ? t('directionDown') : t('directionUp');
            inst.push(t('rightHandStrum', { direction: dir }));
        }
    }

    let bend = false, vib = false, slIn = false, slOut = false, leg = false, pm = false, harm = false, trill = false;
    let ghost = false, dead = false, letR = false, slap = false, pop = false, acc = false;
    for (const n of rawNotes) {
        if (n.hasBend) bend = true;
        if (n.vibrato) vib = true;
        if (n.slideIn) slIn = true;
        if (n.slideOut) slOut = true;
        if (n.isHammer) leg = true;
        if (n.isPalmMute) pm = true;
        if (n.hasHarmonic) harm = true;
        if (n.isTrill) trill = true;
        if (n.isGhost) ghost = true;
        if (n.isDead) dead = true;
        if (n.isLetRing) letR = true;
        if (n.isSlap) slap = true;
        if (n.isPop) pop = true;
        if (n.accent > 0) acc = true;
    }

    if (acc) inst.push(t('accentInstruction'));
    if (ghost) inst.push(t('ghostInstruction'));
    if (dead) inst.push(t('deadInstruction'));
    if (slap) inst.push(t('slapInstruction'));
    if (pop) inst.push(t('popInstruction'));
    if (letR) inst.push(t('letRingInstruction'));
    if (bend) inst.push(t('bendInstruction'));
    if (vib) inst.push(t('vibratoInstruction'));
    if (slIn || slOut) inst.push(t('slideInstruction'));
    if (leg) {
        inst.push(t('legatoInstruction'));
        inst.push(t('tieInstruction'));
    }
    if (pm) inst.push(t('palmMuteInstruction'));
    if (harm) inst.push(t('harmonicInstruction'));
    if (trill) inst.push(t('trillInstruction'));
    if (!inst.length) inst.push(t('plainNoteInstruction'));
    return inst;
}

function buildAnalysis(barIdx, notes, rawNotes, lhMap, rhMap, barre, pos, isTapping, nextData) {
    const LH = [];
    const RH = [];
    for (let i = 0; i < notes.length; i++) {
        const n = notes[i];
        const rn = rawNotes[i];
        const s = n.string;
        const f = n.fret;
        const isDead = n.isDead;
        const sn = STRING_SHORT[s] || String(s);
        let lbl = lhMap[`${s}_${f}`] || '?';
        if (isDead) lbl = t('deadLabel');
        const open = f === 0 && !isDead;
        LH.push({
            finger: open ? '0' : lbl.charAt(0),
            fingerName: open ? t('openStringName') : lbl,
            string: sn,
            stringIndex: s,
            fret: String(f),
            color: open ? '#4FC3F7' : (isDead ? '#757575' : '#D07B30'),
            isDead: !!isDead,
            isHammer: !!rn.isHammer,
            isPullOff: !!rn.isPullOff,
            isSlide: !!rn.slideIn || !!rn.slideOut,
            isVibrato: !!rn.vibrato,
            isGhost: !!rn.isGhost,
            hasBend: !!rn.hasBend,
            isPalmMute: !!rn.isPalmMute,
            hasHarmonic: !!rn.hasHarmonic,
            isTrill: !!rn.isTrill,
            isLetRing: !!rn.isLetRing,
            isSlap: !!rn.isSlap,
            isPop: !!rn.isPop,
            isAccent: !!rn.accent,
            isTapping: !!isTapping
        });
        const rh = rhMap[s] || (s >= 4 ? rightHandName('p') : rightHandName('i'));
        RH.push({ finger: rh.charAt(0), fingerName: rh, string: sn, stringIndex: s, direction: rh.charAt(0), color: s >= 4 ? '#8B6350' : '#3678B5' });
    }

    let contextHint = null;
    let width = 0;
    const fretted = notes.filter(n => n.fret > 0);
    if (fretted.length > 0) width = Math.max(...fretted.map(n => n.fret)) - Math.min(...fretted.map(n => n.fret));

    if (rawNotes.some(r => r.hasHarmonic)) contextHint = t('techniqueHarmonic');
    else if (rawNotes.some(r => r.isDead)) contextHint = t('techniqueDead');
    else if (rawNotes.some(r => r.hasBend)) contextHint = t('techniqueBend');
    else if (rawNotes.some(r => r.isPalmMute)) contextHint = t('techniquePalmMute');
    else if (rawNotes.some(r => r.isTrill)) contextHint = t('techniqueTrill');
    else if (rawNotes.some(r => r.isGhost)) contextHint = t('techniqueGhost');
    else if (rawNotes.some(r => r.isLetRing)) contextHint = t('techniqueLetRing');
    else if (isTapping) contextHint = t('techniqueTapping');
    else if (rawNotes.some(r => r.isSlap)) contextHint = t('techniqueSlap');
    else if (rawNotes.some(r => r.isPop)) contextHint = t('techniquePop');
    else if (rawNotes.some(r => r.isHammer || r.isPullOff)) contextHint = t('techniqueLegato');
    else if (rawNotes.some(r => r.slideIn || r.slideOut)) contextHint = t('techniqueSlide');
    else if (rawNotes.some(r => r.vibrato)) contextHint = t('techniqueVibrato');
    else if (barre !== null) contextHint = t('possibleBarre', { fret: barre });
    else if (width > 3) contextHint = t('wideStretch');

    const nextLH = [];
    if (nextData && nextData.notes) {
        for (const nextNote of nextData.notes) {
            const nextFret = nextNote.fret;
            const nextString = nextNote.string;
            const nextDead = nextNote.isDead;
            const nextLbl = nextData.lhMap?.[`${nextString}_${nextFret}`] || '?';
            nextLH.push({
                finger: nextDead ? 'x' : (nextFret === 0 ? '0' : nextLbl.charAt(0)),
                fingerName: nextDead ? t('deadLabel') : (nextFret === 0 ? t('leftHandOpen', { string: STRING_NAMES[nextString] }) : nextLbl),
                string: STRING_SHORT[nextString] || String(nextString),
                stringIndex: nextString,
                fret: String(nextFret),
                color: nextFret === 0 ? '#4FC3F7' : (nextDead ? '#757575' : '#D07B30'),
                isDead: !!nextDead
            });
        }
    }

    const instructions = buildInstructions(notes, rawNotes, lhMap, rhMap, barre, pos, isTapping);
    return { barIndex: barIdx + 1, leftHand: LH, rightHand: RH, instructions, barreFret: barre, contextHint, nextLeftHand: nextLH };
}

const beatMap = new Map();
const tickArr = [];
let _selectedTrackIndex = 0;
let _transposeSemitones = 0;

function beatKey(beat) {
    try { return `${beat.voice.bar.index}_${beat.index}`; } catch { return null; }
}

function getActiveTrack(score) {
    if (!score?.tracks?.length) return null;
    return score.tracks.find(t => t.index === _selectedTrackIndex) || score.tracks[0] || null;
}

function runFullAnalysis(score) {
    beatMap.clear();
    tickArr.length = 0;
    try {
        const track = getActiveTrack(score);
        if (!track?.staves?.length) return;
        const data = [];
        for (const bar of track.staves[0].bars) {
            for (const voice of bar.voices) {
                if (voice.isEmpty) continue;
                for (const beat of voice.beats) {
                    const notes = beat.notes.filter(n => n.fret != null);
                    if (!notes.length) continue;
                    const durVal = typeof beat.duration === 'object' ? beat.duration.value : beat.duration;
                    data.push({
                        beat,
                        barIdx: bar.index,
                        duration: DUR_BEATS[durVal] ?? 1,
                        notes: notes.map(n => ({ string: normalizeStringIndex(n.string), fret: n.fret, isDead: !!n.isDead })),
                        rawNotes: notes.map(n => ({
                            hasBend: !!n.hasBend,
                            vibrato: !!n.isVibrato,
                            slideIn: !!n.slideInType && n.slideInType !== 0,
                            slideOut: !!n.slideOutType && n.slideOutType !== 0,
                            isHammer: !!n.isHammerPullOrigin || !!n.isHammerPullDestination,
                            isPullOff: !!n.isHammerPullDestination,
                            isPalmMute: !!n.isPalmMute,
                            hasHarmonic: !!n.isHarmonic,
                            isTrill: !!n.isTrill,
                            isGhost: !!n.isGhost,
                            isDead: !!n.isDead,
                            isLetRing: !!n.isLetRing,
                            isSlap: !!n.slap,
                            isPop: !!n.pop,
                            accent: n.accentuated || 0
                        })),
                        isTapping: notes.some(n => !!n.isTapped),
                        isTiedBeat: notes.every(n => !!n.isTieDestination)
                    });
                }
            }
        }

        const positions = dpFingering(data);
        const rhSM = new RightHandSM();
        let prevLhF = {};
        let prevBeatPlacement = {};
        let atkIdx = 0;
        let firstJson = null;

        for (let i = 0; i < data.length; i++) {
            const { beat, barIdx, notes, rawNotes, isTapping, isTiedBeat } = data[i];
            let pos = 1, barre = null, lhMap = {}, newF = {}, rhMap = {};
            if (!(isTiedBeat && atkIdx > 0)) {
                pos = positions[atkIdx] || 1;
                barre = detectBarre(notes);
                const a = assignLhFingers(notes, pos, barre, prevLhF, prevBeatPlacement);
                lhMap = a.lhMap;
                newF = a.newF;
                rhMap = rhSM.getFingers(notes);
                prevLhF = newF;
                const placement = {};
                for (const n of notes) {
                    const key = `${n.string}_${n.fret}`;
                    const label = lhMap[key] || '';
                    const parsed = parseInt(label, 10);
                    if (Number.isFinite(parsed) && parsed > 0) placement[n.string] = { fret: n.fret, finger: parsed };
                }
                prevBeatPlacement = placement;
                atkIdx++;
            }

            const nextData = data[i + 1] && !data[i + 1].isTiedBeat ? (() => {
                const nd = data[i + 1];
                const np = positions[atkIdx] || pos;
                const nb = detectBarre(nd.notes);
                const na = assignLhFingers(nd.notes, np, nb, prevLhF, prevBeatPlacement);
                return { ...nd, lhMap: na.lhMap };
            })() : null;

            const json = JSON.stringify(buildAnalysis(barIdx, notes, rawNotes, lhMap, rhMap, barre, pos, isTapping, nextData));
            const key = beatKey(beat);
            if (key) beatMap.set(key, json);
            tickArr.push({ tick: beat.start ?? beat.absoluteDisplayStart ?? (i * 960), json });
            if (!firstJson && !isTiedBeat) firstJson = json;
        }

        tickArr.sort((a, b) => a.tick - b.tick);
        if (firstJson) postToAndroid(firstJson);

        let compactTabs = '';
        for (const bar of track.staves[0].bars) {
            compactTabs += `Measure ${bar.index + 1}:\n`;
            for (const voice of bar.voices) {
                if (voice.isEmpty) continue;
                let eventNum = 1;
                for (const beat of voice.beats) {
                    const notes = beat.notes.filter(n => n.fret != null);
                    if (!notes.length) continue;
                    const noteLabels = notes.map(n => {
                        const stringIndex = normalizeStringIndex(n.string);
                        const label = n.isDead ? 'x' : n.fret;
                        return t('stringLabel', { string: stringIndex, label, fret: n.fret });
                    });
                    compactTabs += `  Event ${eventNum} inside this measure: ${noteLabels.join(', ')}\n`;
                    eventNum++;
                }
            }
        }
        if (window.Android?.postCompactTabs) window.Android.postCompactTabs(compactTabs);
    } catch (e) {
        console.error('runFullAnalysis:', e);
    }
}

function postToAndroid(json) {
    if (json && window.Android?.postTabAnalysis) window.Android.postTabAnalysis(json);
}

function sendForTick(t) {
    if (!tickArr.length) return;
    let lo = 0, hi = tickArr.length - 1, best = -1;
    while (lo <= hi) {
        const mid = (lo + hi) >> 1;
        if (tickArr[mid].tick <= t) {
            best = mid;
            lo = mid + 1;
        } else hi = mid - 1;
    }
    if (best >= 0) postToAndroid(tickArr[best].json);
}

function sendForBeat(beat) {
    if (!beat) return;
    const key = beatKey(beat);
    if (key && beatMap.has(key)) {
        postToAndroid(beatMap.get(key));
        return;
    }
    const tValue = beat.start ?? beat.absoluteDisplayStart;
    if (tValue != null) sendForTick(tValue);
}
