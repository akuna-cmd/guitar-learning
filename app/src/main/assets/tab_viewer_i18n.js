const STRING_NAMES = {1:'e (1)',2:'b (2)',3:'g (3)',4:'d (4)',5:'A (5)',6:'E (6)'};
const STRING_SHORT = {1:'e',2:'b',3:'g',4:'d',5:'A',6:'E'};
const DUR_BEATS = {1:4,2:2,4:1,8:.5,16:.25,32:.125,64:.0625};
let _locale = 'uk';

const I18N = {
    uk: {
        lhIndex: '1 (вказівний)',
        lhMiddle: '2 (середній)',
        lhRing: '3 (безіменний)',
        lhPinky: '4 (мізинець)',
        rhThumb: 'p (великий)',
        rhIndex: 'i (вказівний)',
        rhMiddle: 'm (середній)',
        rhRing: 'a (безіменний)',
        techniqueHarmonic: 'Флажолет',
        techniqueDead: 'Мертва нота',
        techniqueBend: 'Підтяжка',
        techniquePalmMute: 'Глушіння долонею',
        techniqueTrill: 'Трель',
        techniqueGhost: 'Нота-привид',
        techniqueLetRing: 'Нота звучить далі',
        techniqueTapping: 'Теппінг',
        techniqueSlap: 'Слеп',
        techniquePop: 'Поп',
        techniqueLegato: 'Легато',
        techniqueSlide: 'Слайд',
        techniqueVibrato: 'Вібрато',
        possibleBarre: 'Можливе баре на {fret} ладі',
        wideStretch: 'Широка розтяжка!',
        tappingInstruction: 'Теппінг: Нота виконується ударом пальця правої руки безпосередньо по ладу на грифі.',
        positionBarre: 'Позиція: Можливе баре на {fret}-му ладі.',
        barreHelp: 'За потреби можна притиснути кілька струн вказівним пальцем; якщо незручно, допускається аплікатура без повного баре.',
        positionAround: 'Позиція: Навколо {fret}-го ладу.',
        wideStretchInstruction: 'Широка розтяжка: Великий палець лівої руки слід опустити ближче до центру грифа для полегшення позиції.',
        leftHandMuted: 'Ліва рука: Заглушена нота на струні {string}. Пальці торкаються струни без притискання.',
        leftHandOpen: 'Ліва рука: Відкрита струна {string}.',
        leftHandFinger: 'Ліва рука: Палець {finger} на струні {string}, лад {fret}.',
        rightHandFinger: 'Права рука: Палець {finger}, напрямок удару — {direction}.',
        directionDown: 'вниз',
        directionUp: 'вгору',
        chord: 'Акорд: Одночасне звучання {count} струн.',
        mutedShort: 'глуха',
        openShort: 'відкр.',
        leftHandSummary: 'Ліва рука: {items}.',
        rightHandArpeggio: 'Права рука: Виконується перебором (арпеджіо).',
        rightHandStrum: 'Права рука: Удар по всіх струнах (страм) {direction}.',
        accentInstruction: 'Акцент: Ця нота виконується з підвищеною динамікою (гучніше).',
        ghostInstruction: 'Нота-привид: Виконується дуже тихо, ледь відчутно, створюючи фонову пульсацію.',
        deadInstruction: 'Мертва нота: Струна повністю приглушена лівою рукою; удар правої руки генерує перкусійний звук.',
        slapInstruction: 'Слеп: Виконується різким ударом суглоба великого пальця правої руки по басовій струні.',
        popInstruction: 'Поп: Струна підчіплюється вказівним пальцем правої руки і різко відпускається, створюючи характерний ляскіт.',
        letRingInstruction: 'Нота звучить далі: Струну не слід глушити під час переходу до наступних нот.',
        bendInstruction: 'Підтяжка: Після удару по струні підтягніть її пальцями лівої руки для досягнення потрібної висоти тону.',
        vibratoInstruction: 'Вібрато: Виконується ритмічним розгойдуванням притиснутого пальця для коливання висоти тону.',
        slideInstruction: 'Слайд: Палець ковзає по струні до потрібного ладу без повторного удару медіатором.',
        legatoInstruction: 'Легато: Наступна нота береться без удару правої руки ударом пальця лівої руки по грифу або зривом струни.',
        tieInstruction: 'Ліга: Ноти під дугою виконуються зв’язно, без розриву між ними; перша нота береться атакою, наступні — переважно легато.',
        palmMuteInstruction: 'Глушіння долонею: Ребро правої долоні злегка торкається струн біля підставки, приглушуючи їх резонанс.',
        harmonicInstruction: 'Флажолет: Палець лівої руки злегка торкається струни над порожком без притискання, генеруючи обертон.',
        trillInstruction: 'Трель: Швидке безперервне чергування двох нот методом Hammer-on та Pull-off.',
        plainNoteInstruction: 'Звичайна нота без додаткових артикуляцій.',
        openStringName: 'Відкрита струна',
        deadLabel: 'Глуха (x)',
        stringLabel: 'Струна {string} [{label}] (лад {fret})'
    },
    en: {
        lhIndex: '1 (index)',
        lhMiddle: '2 (middle)',
        lhRing: '3 (ring)',
        lhPinky: '4 (pinky)',
        rhThumb: 'p (thumb)',
        rhIndex: 'i (index)',
        rhMiddle: 'm (middle)',
        rhRing: 'a (ring)',
        techniqueHarmonic: 'Harmonic',
        techniqueDead: 'Dead note',
        techniqueBend: 'Bend',
        techniquePalmMute: 'Palm mute',
        techniqueTrill: 'Trill',
        techniqueGhost: 'Ghost note',
        techniqueLetRing: 'Let ring',
        techniqueTapping: 'Tapping',
        techniqueSlap: 'Slap',
        techniquePop: 'Pop',
        techniqueLegato: 'Legato',
        techniqueSlide: 'Slide',
        techniqueVibrato: 'Vibrato',
        possibleBarre: 'Possible barre on fret {fret}',
        wideStretch: 'Wide stretch!',
        tappingInstruction: 'Tapping: The note is played by striking the fret directly with a finger of the picking hand.',
        positionBarre: 'Position: Possible barre on fret {fret}.',
        barreHelp: 'If needed, press several strings with the index finger; if it feels awkward, a fingering without a full barre is also acceptable.',
        positionAround: 'Position: Around fret {fret}.',
        wideStretchInstruction: 'Wide stretch: Move the thumb of the fretting hand closer to the center of the neck to make the position easier.',
        leftHandMuted: 'Left hand: Muted note on string {string}. The fingers touch the string without pressing it down.',
        leftHandOpen: 'Left hand: Open string {string}.',
        leftHandFinger: 'Left hand: Finger {finger} on string {string}, fret {fret}.',
        rightHandFinger: 'Right hand: Finger {finger}, stroke direction — {direction}.',
        directionDown: 'down',
        directionUp: 'up',
        chord: 'Chord: Simultaneous sound of {count} strings.',
        mutedShort: 'muted',
        openShort: 'open',
        leftHandSummary: 'Left hand: {items}.',
        rightHandArpeggio: 'Right hand: Played as a picked arpeggio.',
        rightHandStrum: 'Right hand: Strum across all strings {direction}.',
        accentInstruction: 'Accent: This note is played with increased emphasis and volume.',
        ghostInstruction: 'Ghost note: Played very quietly, almost imperceptibly, creating a background pulse.',
        deadInstruction: 'Dead note: The string is fully muted by the fretting hand; the picking hand produces a percussive sound.',
        slapInstruction: 'Slap: Performed with a sharp strike of the thumb joint on a bass string.',
        popInstruction: 'Pop: The string is hooked with the index finger of the picking hand and released sharply to create a snap.',
        letRingInstruction: 'Let ring: Allow the note to keep sounding; do not mute the string while moving to the next notes.',
        bendInstruction: 'Bend: After plucking the string, push it with the fretting hand to raise the pitch.',
        vibratoInstruction: 'Vibrato: Performed by rhythmically rocking the fretting finger to vary the pitch slightly.',
        slideInstruction: 'Slide: The finger glides along the string to the target fret without another pick attack.',
        legatoInstruction: 'Legato: The next note is taken without a new right-hand attack, using a hammer-on or pull-off.',
        tieInstruction: 'Slur: Notes under the arc are played smoothly without separation; the first note has the attack, the next notes are mostly legato.',
        palmMuteInstruction: 'Palm mute: The edge of the picking hand lightly touches the strings near the bridge to damp their resonance.',
        harmonicInstruction: 'Harmonic: The fretting finger lightly touches the string above the fret without pressing it down, producing an overtone.',
        trillInstruction: 'Trill: Rapid continuous alternation of two notes using hammer-ons and pull-offs.',
        plainNoteInstruction: 'Regular note without additional articulation.',
        openStringName: 'Open string',
        deadLabel: 'Dead (x)',
        stringLabel: 'String {string} [{label}] (fret {fret})'
    }
};

function activeLocale() {
    return (_locale || 'uk').toLowerCase().startsWith('en') ? 'en' : 'uk';
}

function t(key, params = {}) {
    const table = I18N[activeLocale()] || I18N.uk;
    let value = table[key] || I18N.uk[key] || key;
    for (const [paramKey, paramValue] of Object.entries(params)) {
        value = value.replaceAll(`{${paramKey}}`, String(paramValue));
    }
    return value;
}

function leftHandName(finger) {
    const names = {
        1: t('lhIndex'),
        2: t('lhMiddle'),
        3: t('lhRing'),
        4: t('lhPinky')
    };
    return names[finger] || String(finger);
}

function rightHandName(symbol) {
    const names = {
        p: t('rhThumb'),
        i: t('rhIndex'),
        m: t('rhMiddle'),
        a: t('rhRing')
    };
    return names[symbol] || symbol;
}
