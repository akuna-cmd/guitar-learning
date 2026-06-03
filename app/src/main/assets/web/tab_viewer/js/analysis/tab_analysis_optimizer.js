function buildBeatCandidates(beatData) {
    const leftHandCandidates = generateLeftHandCandidates(beatData);
    const rightHandCandidates = generateRightHandCandidates(beatData);
    const combined = [];

    for (const leftCandidate of leftHandCandidates) {
        for (const rightCandidate of rightHandCandidates) {
            combined.push({
                ...beatData,
                ...leftCandidate,
                ...rightCandidate,
                stateCost: leftCandidate.leftHandStateCost + rightCandidate.rightHandStateCost,
                stateCostBreakdown: {
                    leftHand: leftCandidate.leftHandStateBreakdown,
                    rightHand: {
                        patternCost: rightCandidate.rightHandStateCost
                    }
                }
            });
        }
    }

    return combined
        .sort((a, b) => a.stateCost - b.stateCost)
        .slice(0, 20);
}

function isSingleSameStringMovement(prevCandidate, candidate) {
    return prevCandidate.notes.length === 1 &&
        candidate.notes.length === 1 &&
        !prevCandidate.notes[0].isTapped &&
        !candidate.notes[0].isTapped &&
        prevCandidate.notes[0].string === candidate.notes[0].string;
}

function isSequentialSingleNoteMove(prevCandidate, candidate) {
    return prevCandidate.notes.length === 1 &&
        candidate.notes.length === 1 &&
        !prevCandidate.notes[0].isDead &&
        !candidate.notes[0].isDead &&
        !prevCandidate.notes[0].isTapped &&
        !candidate.notes[0].isTapped &&
        prevCandidate.notes[0].fret > 0 &&
        candidate.notes[0].fret > 0;
}

function computeRightHandTransitionCost(prevCandidate, candidate, speedFactor) {
    const breakdown = {
        trebleRepeat: 0,
        trebleAlternation: 0,
        thumbRepeat: 0,
        thumbLeap: 0,
        blockAdjacency: 0
    };
    let cost = 0;

    if (prevCandidate.rightHandPattern === 'singleTreble' && candidate.rightHandPattern === 'singleTreble') {
        if (prevCandidate.primaryTrebleFinger === candidate.primaryTrebleFinger) {
            const delta = speedFactor > 1.3 ? 1.15 : 0.2;
            breakdown.trebleRepeat += delta;
            cost += delta;
        } else {
            const delta = speedFactor > 1.3 ? -0.55 : -0.15;
            breakdown.trebleAlternation += delta;
            cost += delta;
        }
    }

    const prevBass = prevCandidate.rightHandBassSequence || [];
    const nextBass = candidate.rightHandBassSequence || [];
    if (prevBass.length === 1 && nextBass.length === 1) {
        const stringDelta = Math.abs(prevBass[0].string - nextBass[0].string);
        if (stringDelta >= 2 && speedFactor > 1.1) {
            const delta = 0.45 * stringDelta;
            breakdown.thumbLeap += delta;
            cost += delta;
        } else if (stringDelta === 1 && speedFactor > 1.1) {
            const delta = -0.1;
            breakdown.thumbRepeat += delta;
            cost += delta;
        }
    }

    const nextTreble = candidate.rightHandTrebleSequence || [];
    if (nextTreble.length >= 2) {
        let adjacencyPenalty = 0;
        for (let i = 1; i < nextTreble.length; i++) {
            const prev = nextTreble[i - 1];
            const current = nextTreble[i];
            const sameFinger = prev.finger === current.finger;
            const fingerJump =
                (prev.finger === 'i' && current.finger === 'a') ||
                (prev.finger === 'a' && current.finger === 'i');
            if (sameFinger) adjacencyPenalty += 1.4;
            if (fingerJump) adjacencyPenalty += 0.5;
        }
        if (adjacencyPenalty) {
            breakdown.blockAdjacency += adjacencyPenalty;
            cost += adjacencyPenalty;
        }
    }

    return { cost, breakdown };
}

function computeTransitionCost(prevCandidate, candidate) {
    const breakdown = {
        positionShift: 0,
        barreChange: 0,
        commonShape: 0,
        letRing: 0,
        slide: 0,
        legato: 0,
        sameFretStringMove: 0,
        rightHand: 0,
        chordPattern: 0
    };
    const speedFactor = Math.min(prevCandidate.duration, candidate.duration) <= 0.5 ? 1.7
        : Math.min(prevCandidate.duration, candidate.duration) <= 1 ? 1.2
        : 0.9;
    const prevFretted = activeFrets(prevCandidate.notes);
    const nextFretted = activeFrets(candidate.notes);
    const currentMostlyOpen = nextFretted.length === 0 || nextFretted.every(fret => fret <= 2);
    breakdown.positionShift = Math.abs(candidate.position - prevCandidate.position) * (currentMostlyOpen ? 0.6 : 1.5) * speedFactor;
    let cost = breakdown.positionShift;

    if (barreKey(candidate.barre) !== barreKey(prevCandidate.barre)) {
        breakdown.barreChange += 0.9;
        cost += 0.9;
    }
    if (candidate.position === prevCandidate.position) {
        breakdown.positionShift -= 0.45;
        cost -= 0.45;
    }

    const commonPairs = countCommonFrettedPairs(prevCandidate.notes, candidate.notes);
    breakdown.commonShape -= Math.min(1.4, commonPairs * 0.7);
    cost += breakdown.commonShape;

    if (prevCandidate.rawNotes.some(note => note.isLetRing) || candidate.rawNotes.some(note => note.isLetRing)) {
        breakdown.letRing -= Math.min(0.8, commonPairs * 0.4);
        cost += breakdown.letRing;
    }

    if (isSingleSameStringMovement(prevCandidate, candidate)) {
        const prevNote = prevCandidate.notes[0];
        const nextNote = candidate.notes[0];
        const fretDelta = Math.abs(prevNote.fret - nextNote.fret);
        const prevFinger = prevCandidate.leftHandPlacement[prevNote.string]?.finger;
        const nextFinger = candidate.leftHandPlacement[nextNote.string]?.finger;
        const slideLike = candidate.rawNotes.some(note => note.slideIn || note.slideOut) ||
            prevCandidate.rawNotes.some(note => note.slideIn || note.slideOut);
        const legatoLike = candidate.rawNotes.some(note => note.isHammer || note.isPullOff) ||
            prevCandidate.rawNotes.some(note => note.isHammer || note.isPullOff);

        if (slideLike && fretDelta <= 4) {
            breakdown.slide += prevFinger === nextFinger ? -1.7 : 1.1;
            cost += prevFinger === nextFinger ? -1.7 : 1.1;
        }
        const legatoType = candidate.rawNotes.find(note => note.legatoType)?.legatoType ||
            prevCandidate.rawNotes.find(note => note.legatoType)?.legatoType ||
            null;
        if (legatoLike && fretDelta <= 2) {
            const legatoDelta =
                legatoType === 'hammer' ? (prevFinger !== nextFinger ? -1.1 : 0.55) :
                legatoType === 'pullOff' ? (prevFinger !== nextFinger ? -1.3 : 0.6) :
                (prevFinger !== nextFinger ? -1.0 : 0.4);
            breakdown.legato += legatoDelta;
            cost += legatoDelta;
        }
    }

    if (isSequentialSingleNoteMove(prevCandidate, candidate)) {
        const prevNote = prevCandidate.notes[0];
        const nextNote = candidate.notes[0];
        const prevFinger = prevCandidate.leftHandPlacement[prevNote.string]?.finger;
        const nextFinger = candidate.leftHandPlacement[nextNote.string]?.finger;
        const stringDelta = Math.abs(prevNote.string - nextNote.string);
        const sameFret = prevNote.fret === nextNote.fret;
        const sameFinger = prevFinger != null && prevFinger === nextFinger;
        const slideLike = candidate.rawNotes.some(note => note.slideIn || note.slideOut) ||
            prevCandidate.rawNotes.some(note => note.slideIn || note.slideOut);
        const tiedLike = candidate.rawNotes.some(note => note.isLetRing || note.isHammer || note.isPullOff) ||
            prevCandidate.rawNotes.some(note => note.isLetRing || note.isHammer || note.isPullOff);

        if (!slideLike && !tiedLike && sameFret && sameFinger && stringDelta >= 1) {
            const delta = stringDelta === 1 ? 5.2 : 6.8;
            breakdown.sameFretStringMove += delta;
            cost += delta;
        }

        if (!slideLike && !tiedLike && sameFret && stringDelta === 1 && prevFinger !== nextFinger) {
            breakdown.sameFretStringMove -= 1.1;
            cost -= 1.1;
        }
    }

    const rightHandTransition = computeRightHandTransitionCost(prevCandidate, candidate, speedFactor);
    breakdown.rightHand += rightHandTransition.cost;
    breakdown.rightHandDetails = rightHandTransition.breakdown;
    cost += rightHandTransition.cost;

    if (candidate.rightHandPattern === 'pinch' || candidate.rightHandPattern === 'fingerstyleChord') {
        breakdown.chordPattern -= 0.25;
        cost -= 0.25;
    }

    return { cost, breakdown };
}

function optimizeBeatSequence(beatData) {
    const candidateMatrix = beatData.map(data => buildBeatCandidates(data));
    if (!candidateMatrix.length) return [];

    const scores = candidateMatrix.map(() => []);
    const backPointers = candidateMatrix.map(() => []);

    for (let i = 0; i < candidateMatrix[0].length; i++) {
        scores[0][i] = candidateMatrix[0][i].stateCost;
        backPointers[0][i] = -1;
    }

    for (let beatIndex = 1; beatIndex < candidateMatrix.length; beatIndex++) {
        const candidates = candidateMatrix[beatIndex];
        const previousCandidates = candidateMatrix[beatIndex - 1];
        for (let currentIndex = 0; currentIndex < candidates.length; currentIndex++) {
            let bestScore = Number.POSITIVE_INFINITY;
            let bestPrevIndex = 0;

            for (let prevIndex = 0; prevIndex < previousCandidates.length; prevIndex++) {
                const transition = computeTransitionCost(previousCandidates[prevIndex], candidates[currentIndex]);
                const score = scores[beatIndex - 1][prevIndex] +
                    candidates[currentIndex].stateCost +
                    transition.cost;

                if (score < bestScore) {
                    bestScore = score;
                    bestPrevIndex = prevIndex;
                    candidates[currentIndex].transitionCostBreakdown = transition.breakdown;
                    candidates[currentIndex].totalPathScore = bestScore;
                }
            }

            scores[beatIndex][currentIndex] = bestScore;
            backPointers[beatIndex][currentIndex] = bestPrevIndex;
        }
    }

    let bestIndex = 0;
    let bestScore = Number.POSITIVE_INFINITY;
    const lastScores = scores[scores.length - 1];
    for (let i = 0; i < lastScores.length; i++) {
        if (lastScores[i] < bestScore) {
            bestScore = lastScores[i];
            bestIndex = i;
        }
    }

    const selected = new Array(candidateMatrix.length);
    for (let beatIndex = candidateMatrix.length - 1; beatIndex >= 0; beatIndex--) {
        selected[beatIndex] = candidateMatrix[beatIndex][bestIndex];
        bestIndex = backPointers[beatIndex][bestIndex];
        if (bestIndex < 0) break;
    }

    return selected.map((candidate, index) => ({
        ...candidate,
        performanceHints: derivePerformanceHints(
            selected[index - 1] || null,
            candidate,
            selected[index + 1] || null
        )
    }));
}

function derivePerformanceHints(prevCandidate, candidate, nextCandidate) {
    const hints = [];

    if (candidate.rightHandPattern === 'singleTreble' && nextCandidate?.rightHandPattern === 'singleTreble') {
        const quickPassage = Math.min(candidate.duration, nextCandidate.duration) <= 0.5;
        const currentNote = candidate.notes[0];
        const nextNote = nextCandidate.notes[0];
        if (quickPassage &&
            candidate.primaryTrebleFinger &&
            nextCandidate.primaryTrebleFinger &&
            candidate.primaryTrebleFinger !== nextCandidate.primaryTrebleFinger &&
            currentNote && nextNote &&
            !currentNote.isDead &&
            !nextNote.isDead) {
            hints.push(t('rightHandAlternatingTreble', {
                first: rightHandName(candidate.primaryTrebleFinger),
                second: rightHandName(nextCandidate.primaryTrebleFinger)
            }));
        }
    }

    if (candidate.rightHandPattern === 'pinch' || candidate.rightHandPattern === 'fingerstyleChord') {
        hints.push(t('rightHandBassMelody'));
    } else if (candidate.rightHandPattern === 'trebleBlock') {
        hints.push(t('rightHandTrebleBlock'));
    }

    if (nextCandidate) {
        const sameShape = candidate.position === nextCandidate.position &&
            countCommonFrettedPairs(candidate.notes, nextCandidate.notes) > 0;
        if (sameShape) {
            hints.push(t('leftHandKeepShape'));
        } else if (Math.abs(nextCandidate.position - candidate.position) >= 3 && candidate.duration >= 0.5) {
            hints.push(t('leftHandShiftLate'));
        }
    }

    if (prevCandidate && candidate.rawNotes.some(note => note.isLetRing) &&
        countCommonFrettedPairs(prevCandidate.notes, candidate.notes) > 0) {
        hints.push(t('leftHandKeepShape'));
    }

    return [...new Set(hints)];
}
