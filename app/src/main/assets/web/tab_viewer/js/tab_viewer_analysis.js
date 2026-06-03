(function() {
    window.TabViewerAnalysis = {
        runFullAnalysis,
        sendForTick,
        sendForBeat,
        getActiveTrack,
        extractBeatData,
        optimizeBeatSequence,
        ensureBody,
        get selectedTrackIndex() {
            return _selectedTrackIndex;
        },
        set selectedTrackIndex(value) {
            _selectedTrackIndex = value;
        },
        get transposeSemitones() {
            return _transposeSemitones;
        },
        set transposeSemitones(value) {
            _transposeSemitones = value;
        }
    };
})();
