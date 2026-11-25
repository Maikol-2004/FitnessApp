package com.maikol.fitnessapp.feature.training.catalog

import java.text.Normalizer

/*
 * G1 (docs/13_EXERCISE_LIBRARY.md): catálogo local y versionado de ejercicios de repeticiones.
 * - Los IDs son inmutables y no dependen del texto: renombrar o traducir no cambia la identidad.
 * - Texto original en español, redactado para este proyecto. Sin medios de demostración: la ficha es
 *   «Guía en texto» (media = null) hasta disponer de recursos con licencia documentada.
 * - El catálogo nunca reescribe rutinas ni sesiones: estas guardan su propio nombre (snapshot).
 */

enum class MuscleGroup(val label: String) {
    CHEST("Pecho"),
    BACK("Espalda"),
    SHOULDERS("Hombros"),
    BICEPS("Bíceps"),
    TRICEPS("Tríceps"),
    QUADS("Cuádriceps"),
    HAMSTRINGS("Isquiotibiales"),
    GLUTES("Glúteos"),
    CALVES("Gemelos"),
    CORE("Abdomen/core"),
}

enum class Equipment(val label: String) {
    BODYWEIGHT("Peso corporal"),
    DUMBBELL("Mancuernas"),
    BARBELL("Barra"),
    MACHINE("Máquina"),
    CABLE("Polea"),
    BAND("Bandas"),
    BENCH("Banco"),
}

enum class Difficulty(val label: String, val description: String) {
    BEGINNER("Principiante", "Movimiento sencillo y estable; buen punto de partida."),
    INTERMEDIATE("Intermedio", "Requiere algo de práctica para coordinar postura y recorrido."),
    ADVANCED("Avanzado", "Técnica exigente; conviene aprenderlo con poca carga y, si es posible, supervisión."),
}

data class CatalogExercise(
    val id: String,
    val name: String,
    val aliases: List<String>,
    val primary: MuscleGroup,
    val secondary: List<MuscleGroup>,
    /** Equipo imprescindible. */
    val equipment: List<Equipment>,
    /** Equipo de apoyo (banco, colchoneta…) que no define el ejercicio. */
    val accessories: List<Equipment>,
    val difficulty: Difficulty,
    /** Multiarticular: la plantilla inicial usa menos repeticiones y más descanso. */
    val compound: Boolean,
    val preparation: String,
    val steps: List<String>,
    val breathing: String,
    val mistakes: List<String>,
    /** Recurso visual con licencia documentada; null = guía solo en texto. */
    val media: String? = null,
) {
    val muscles: List<MuscleGroup> get() = listOf(primary) + secondary
    val allEquipment: List<Equipment> get() = equipment + accessories
}

/** Plantilla editable al añadir desde el catálogo: no es un registro ni una recomendación personal. */
data class SetTemplate(val sets: Int, val reps: Int, val restSeconds: Int)

fun CatalogExercise.template(): SetTemplate =
    if (compound) SetTemplate(sets = 3, reps = 8, restSeconds = 120) else SetTemplate(sets = 3, reps = 12, restSeconds = 60)

/** Minúsculas y sin tildes/diéresis: «Bíceps» y «biceps» son iguales. */
fun normalizeForSearch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()
        .trim()

/**
 * Búsqueda y filtros. Cada palabra de la consulta debe aparecer en el nombre, un alias o el músculo
 * principal; músculo y equipo se combinan con AND. El filtro muscular incluye secundarios, pero se ordenan
 * primero los que lo trabajan como principal (orden estable del catálogo en cada bloque).
 */
fun List<CatalogExercise>.search(query: String, muscle: MuscleGroup?, equipment: Equipment?): List<CatalogExercise> {
    val words = normalizeForSearch(query).split(Regex("\\s+")).filter { it.isNotEmpty() }
    return filter { e ->
        (muscle == null || muscle in e.muscles) &&
            (equipment == null || equipment in e.allEquipment) &&
            (words.isEmpty() || e.searchText().let { text -> words.all { it in text } })
    }.sortedBy { if (muscle != null && it.primary != muscle) 1 else 0 }
}

private fun CatalogExercise.searchText(): String =
    normalizeForSearch((listOf(name, primary.label) + aliases).joinToString(" "))

object ExerciseCatalog {
    /** Versión del contenido; subirla al cambiar textos. Nunca reutilizar ni cambiar un ID existente. */
    const val VERSION = 1

    fun byId(id: String?): CatalogExercise? = id?.let { index[it] }

    val all: List<CatalogExercise> = listOf(
        // ---------------- Pecho ----------------
        ex(
            "chest_barbell_bench_press", "Press banca con barra", listOf("Press de banca", "Bench press", "Press plano"),
            MuscleGroup.CHEST, listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS), listOf(Equipment.BARBELL), listOf(Equipment.BENCH),
            Difficulty.INTERMEDIATE, compound = true,
            prep = "Túmbate con los ojos bajo la barra, pies firmes en el suelo y omóplatos juntos y hacia abajo. Agarra la barra algo más abierto que los hombros.",
            steps = listOf(
                "Saca la barra del soporte con los brazos estirados y llévala sobre la línea de los hombros.",
                "Bájala con control hasta rozar la parte baja del pecho, con los codos a unos 45° del cuerpo.",
                "Empuja hacia arriba y ligeramente hacia atrás hasta estirar los brazos sin bloquear de golpe.",
                "Mantén glúteos en el banco y omóplatos apretados durante toda la serie.",
            ),
            breathing = "Toma aire antes de bajar y mantenlo; suéltalo al superar la parte más dura de la subida.",
            mistakes = listOf(
                "Rebotar la barra en el pecho para ganar impulso.",
                "Abrir los codos a 90°, lo que carga en exceso el hombro.",
                "Despegar los glúteos del banco al empujar.",
            ),
        ),
        ex(
            "chest_dumbbell_bench_press", "Press banca con mancuernas", listOf("Press plano con mancuernas", "Press de pecho con mancuernas"),
            MuscleGroup.CHEST, listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS), listOf(Equipment.DUMBBELL), listOf(Equipment.BENCH),
            Difficulty.BEGINNER, compound = true,
            prep = "Siéntate con las mancuernas sobre los muslos, túmbate llevándolas al pecho y apoya bien los pies.",
            steps = listOf(
                "Empuja las mancuernas hacia arriba hasta tener los brazos estirados sobre el pecho.",
                "Bájalas abriendo los codos en diagonal hasta notar un estiramiento cómodo en el pecho.",
                "Súbelas juntándolas ligeramente arriba sin que choquen.",
            ),
            breathing = "Inspira al bajar y espira al empujar.",
            mistakes = listOf(
                "Dejar caer las mancuernas al final del recorrido sin control.",
                "Bajar tanto que el hombro se adelanta y pierde la posición.",
                "Llevar cada mancuerna a una altura distinta.",
            ),
        ),
        ex(
            "chest_incline_dumbbell_press", "Press inclinado con mancuernas", listOf("Press inclinado", "Incline press"),
            MuscleGroup.CHEST, listOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS), listOf(Equipment.DUMBBELL), listOf(Equipment.BENCH),
            Difficulty.BEGINNER, compound = true,
            prep = "Inclina el respaldo entre 30° y 45°. Apoya espalda y cabeza y sujeta las mancuernas a la altura de la parte alta del pecho.",
            steps = listOf(
                "Empuja hacia arriba siguiendo una línea vertical sobre la clavícula.",
                "Baja despacio hasta que los codos queden algo por debajo del banco.",
                "Vuelve a subir manteniendo las muñecas rectas encima de los codos.",
            ),
            breathing = "Inspira en la bajada y espira en la subida.",
            mistakes = listOf(
                "Inclinar demasiado el banco, convirtiéndolo en un press de hombro.",
                "Arquear mucho la zona lumbar para despegar el pecho.",
            ),
        ),
        ex(
            "chest_machine_fly", "Aperturas en máquina", listOf("Pec deck", "Contractor de pecho", "Mariposa"),
            MuscleGroup.CHEST, listOf(MuscleGroup.SHOULDERS), listOf(Equipment.MACHINE), emptyList(),
            Difficulty.BEGINNER, compound = false,
            prep = "Ajusta el asiento para que las asas queden a la altura del pecho. Espalda apoyada y codos ligeramente flexionados.",
            steps = listOf(
                "Junta las asas delante del pecho describiendo un arco amplio.",
                "Aprieta el pecho un instante con las manos juntas.",
                "Abre despacio hasta notar estiramiento sin que los brazos vayan por detrás del cuerpo.",
            ),
            breathing = "Espira al cerrar y inspira al abrir.",
            mistakes = listOf(
                "Estirar y doblar los codos durante el recorrido, convirtiéndolo en un empuje.",
                "Despegar la espalda del respaldo al cerrar.",
            ),
        ),
        ex(
            "chest_push_up", "Flexiones", listOf("Push up", "Lagartijas", "Fondos en el suelo"),
            MuscleGroup.CHEST, listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS, MuscleGroup.CORE), listOf(Equipment.BODYWEIGHT), emptyList(),
            Difficulty.BEGINNER, compound = true,
            prep = "Manos en el suelo algo más abiertas que los hombros y cuerpo en línea recta de la cabeza a los talones. Si cuesta, apoya las rodillas o las manos en un banco.",
            steps = listOf(
                "Baja el pecho hacia el suelo doblando los codos en diagonal.",
                "Detente cuando el pecho quede a pocos centímetros del suelo.",
                "Empuja el suelo hasta estirar los brazos manteniendo el cuerpo rígido.",
            ),
            breathing = "Inspira al bajar y espira al subir.",
            mistakes = listOf(
                "Dejar caer la cadera y hundir la zona lumbar.",
                "Hacer medio recorrido moviendo solo la cabeza.",
                "Colocar las manos muy adelantadas respecto a los hombros.",
            ),
        ),
        // ---------------- Espalda ----------------
        ex(
            "back_lat_pulldown", "Jalón al pecho", listOf("Jalón en polea alta", "Lat pulldown", "Polea al pecho"),
            MuscleGroup.BACK, listOf(MuscleGroup.BICEPS), listOf(Equipment.CABLE, Equipment.MACHINE), emptyList(),
            Difficulty.BEGINNER, compound = true,
            prep = "Ajusta el rodillo para fijar los muslos. Agarra la barra algo más abierto que los hombros y siéntate con el pecho alto.",
            steps = listOf(
                "Lleva los hombros hacia abajo antes de doblar los codos.",
                "Tira de la barra hacia la parte alta del pecho dirigiendo los codos al suelo.",
                "Sube la barra con control hasta estirar los brazos por completo.",
            ),
            breathing = "Espira al tirar y inspira al subir la barra.",
            mistakes = listOf(
                "Echarse muy atrás y tirar con el peso del cuerpo.",
                "Llevar la barra por detrás de la nuca.",
                "Soltar la barra de golpe en la subida.",
            ),
        ),
        ex(
            "back_seated_cable_row", "Remo sentado en polea", listOf("Remo en polea baja", "Seated row", "Remo gironda"),
            MuscleGroup.BACK, listOf(MuscleGroup.BICEPS), listOf(Equipment.CABLE), emptyList(),
            Difficulty.BEGINNER, compound = true,
            prep = "Siéntate con los pies en la plataforma y las rodillas algo dobladas. Sujeta el agarre con la espalda recta y el torso casi vertical.",
            steps = listOf(
                "Tira del agarre hacia el abdomen llevando los codos pegados hacia atrás.",
                "Junta los omóplatos al final sin encoger los hombros.",
                "Estira los brazos despacio dejando que los omóplatos se separen.",
            ),
            breathing = "Espira al tirar e inspira al volver.",
            mistakes = listOf(
                "Balancear el torso adelante y atrás para mover el peso.",
                "Encoger los hombros hacia las orejas.",
            ),
        ),
        ex(
            "back_one_arm_dumbbell_row", "Remo con mancuerna", listOf("Remo a una mano", "Remo unilateral"),
            MuscleGroup.BACK, listOf(MuscleGroup.BICEPS), listOf(Equipment.DUMBBELL), listOf(Equipment.BENCH),
            Difficulty.BEGINNER, compound = true,
            prep = "Apoya una mano y una rodilla en el banco y el otro pie en el suelo. Espalda plana y mancuerna colgando bajo el hombro.",
            steps = listOf(
                "Tira de la mancuerna hacia la cadera con el codo cerca del cuerpo.",
                "Para cuando el codo pase la línea de la espalda.",
                "Baja la mancuerna con control hasta estirar el brazo.",
                "Completa las repeticiones y cambia de lado.",
            ),
            breathing = "Espira al subir la mancuerna e inspira al bajarla.",
            mistakes = listOf(
                "Girar el tronco hacia arriba para subir más.",
                "Tirar hacia el hombro en lugar de hacia la cadera.",
                "Redondear la espalda.",
            ),
        ),
        ex(
            "back_assisted_pull_up", "Dominada asistida", listOf("Dominadas asistidas", "Assisted pull up"),
            MuscleGroup.BACK, listOf(MuscleGroup.BICEPS, MuscleGroup.CORE), listOf(Equipment.MACHINE), listOf(Equipment.BAND),
            Difficulty.INTERMEDIATE, compound = true,
            prep = "En la máquina, elige la asistencia (más peso de contrapeso = más ayuda) y apoya las rodillas en la plataforma. Con banda, pásala por la barra y apoya un pie o rodilla.",
            steps = listOf(
                "Cuelga con los brazos estirados y agarre algo más abierto que los hombros.",
                "Baja los hombros y sube llevando el pecho hacia la barra.",
                "Supera la barra con la barbilla sin estirar el cuello.",
                "Desciende despacio hasta estirar los brazos.",
            ),
            breathing = "Espira al subir e inspira al bajar.",
            mistakes = listOf(
                "Patalear o balancearse para subir.",
                "Quedarse a medio recorrido abajo, sin estirar los brazos.",
            ),
        ),
        ex(
            "back_chest_supported_row", "Remo con apoyo de pecho", listOf("Remo en banco inclinado", "Remo en máquina con apoyo", "Chest supported row"),
            MuscleGroup.BACK, listOf(MuscleGroup.BICEPS, MuscleGroup.SHOULDERS), listOf(Equipment.DUMBBELL), listOf(Equipment.BENCH),
            Difficulty.BEGINNER, compound = true,
            prep = "Túmbate boca abajo en un banco inclinado unos 30°, con el pecho apoyado y una mancuerna en cada mano colgando.",
            steps = listOf(
                "Tira de las mancuernas hacia los lados del abdomen.",
                "Junta los omóplatos arriba manteniendo el pecho en el respaldo.",
                "Baja con control hasta estirar los brazos.",
            ),
            breathing = "Espira al tirar e inspira al bajar.",
            mistakes = listOf(
                "Levantar el pecho del banco para ayudarse con la espalda baja.",
                "Mover solo los antebrazos sin desplazar los omóplatos.",
            ),
        ),
        // ---------------- Hombros ----------------
        ex(
            "shoulders_seated_dumbbell_press", "Press sentado con mancuernas", listOf("Press de hombros", "Press militar con mancuernas", "Shoulder press"),
            MuscleGroup.SHOULDERS, listOf(MuscleGroup.TRICEPS), listOf(Equipment.DUMBBELL), listOf(Equipment.BENCH),
            Difficulty.BEGINNER, compound = true,
            prep = "Respaldo casi vertical. Siéntate con la espalda apoyada y las mancuernas a la altura de las orejas, palmas al frente.",
            steps = listOf(
                "Empuja las mancuernas hacia arriba hasta casi estirar los brazos.",
                "Acércalas ligeramente arriba sin chocarlas.",
                "Baja con control hasta que queden a la altura de las orejas.",
            ),
            breathing = "Espira al empujar e inspira al bajar.",
            mistakes = listOf(
                "Arquear la zona lumbar separándola del respaldo.",
                "Bajar solo unos centímetros y acortar el recorrido.",
            ),
        ),
        ex(
            "shoulders_dumbbell_lateral_raise", "Elevaciones laterales", listOf("Elevación lateral con mancuernas", "Lateral raise", "Vuelos laterales"),
            MuscleGroup.SHOULDERS, emptyList(), listOf(Equipment.DUMBBELL), emptyList(),
            Difficulty.BEGINNER, compound = false,
            prep = "De pie, pies a la anchura de la cadera, una mancuerna ligera en cada mano junto a los muslos y codos un poco doblados.",
            steps = listOf(
                "Sube los brazos hacia los lados como si alejaras las mancuernas del cuerpo.",
                "Para a la altura de los hombros con los codos un poco por encima de las muñecas.",
                "Baja despacio, sin dejar caer los brazos.",
            ),
            breathing = "Espira al subir e inspira al bajar.",
            mistakes = listOf(
                "Dar un tirón con la espalda para levantar más peso.",
                "Subir las manos por encima de los hombros encogiendo el trapecio.",
                "Usar una carga que obliga a doblar mucho los codos.",
            ),
        ),
        ex(
            "shoulders_cable_lateral_raise", "Elevaciones laterales en polea", listOf("Elevación lateral en polea", "Cable lateral raise"),
            MuscleGroup.SHOULDERS, emptyList(), listOf(Equipment.CABLE), emptyList(),
            Difficulty.INTERMEDIATE, compound = false,
            prep = "Polea a la altura más baja. Ponte de lado y sujeta el agarre con la mano más alejada; el cable pasa por delante del cuerpo.",
            steps = listOf(
                "Con el codo algo flexionado, separa el brazo del cuerpo hacia el lateral.",
                "Sube hasta la altura del hombro manteniendo el torso quieto.",
                "Baja despacio resistiendo la tensión del cable.",
                "Termina la serie y repite con el otro brazo.",
            ),
            breathing = "Espira al elevar e inspira al bajar.",
            mistakes = listOf(
                "Inclinarse hacia el lado contrario para ayudarse.",
                "Girar la muñeca y subir con el pulgar hacia arriba como si fuera un frontal.",
            ),
        ),
        ex(
            "shoulders_dumbbell_rear_delt_fly", "Pájaros con mancuernas", listOf("Pájaro", "Elevaciones posteriores", "Rear delt fly"),
            MuscleGroup.SHOULDERS, listOf(MuscleGroup.BACK), listOf(Equipment.DUMBBELL), emptyList(),
            Difficulty.INTERMEDIATE, compound = false,
            prep = "De pie, inclina el tronco hacia delante desde la cadera con la espalda recta y rodillas suaves. Mancuernas colgando bajo el pecho.",
            steps = listOf(
                "Abre los brazos hacia los lados con los codos ligeramente flexionados.",
                "Sube hasta que los brazos queden en línea con la espalda.",
                "Baja con control sin perder la inclinación del tronco.",
            ),
            breathing = "Espira al abrir e inspira al cerrar.",
            mistakes = listOf(
                "Incorporar el tronco en cada repetición.",
                "Juntar mucho los omóplatos y convertirlo en un remo.",
            ),
        ),
        ex(
            "shoulders_reverse_machine_fly", "Aperturas inversas en máquina", listOf("Pec deck inverso", "Reverse fly", "Contractor inverso"),
            MuscleGroup.SHOULDERS, listOf(MuscleGroup.BACK), listOf(Equipment.MACHINE), emptyList(),
            Difficulty.BEGINNER, compound = false,
            prep = "Siéntate mirando al respaldo, con el pecho apoyado. Ajusta las asas a la altura de los hombros y agárralas con los brazos estirados al frente.",
            steps = listOf(
                "Lleva los brazos hacia atrás en un arco amplio.",
                "Para cuando las manos estén en línea con los hombros.",
                "Vuelve al frente despacio sin que las placas choquen.",
            ),
            breathing = "Espira al abrir e inspira al volver.",
            mistakes = listOf(
                "Separar el pecho del respaldo para usar impulso.",
                "Doblar los codos al final para tirar con los brazos.",
            ),
        ),
        // ---------------- Bíceps ----------------
        ex(
            "biceps_dumbbell_curl", "Curl con mancuernas", listOf("Curl de bíceps", "Curl alterno", "Dumbbell curl"),
            MuscleGroup.BICEPS, emptyList(), listOf(Equipment.DUMBBELL), emptyList(),
            Difficulty.BEGINNER, compound = false,
            prep = "De pie, una mancuerna en cada mano con las palmas hacia delante y los codos junto al cuerpo.",
            steps = listOf(
                "Dobla los codos subiendo las mancuernas hacia los hombros.",
                "Aprieta el bíceps arriba sin adelantar los codos.",
                "Baja despacio hasta estirar casi del todo los brazos.",
            ),
            breathing = "Espira al subir e inspira al bajar.",
            mistakes = listOf(
                "Balancear la cadera para levantar la mancuerna.",
                "Separar los codos del cuerpo y llevarlos hacia delante.",
            ),
        ),
        ex(
            "biceps_ez_bar_curl", "Curl con barra EZ", listOf("Curl con barra Z", "Curl barra W", "EZ curl"),
            MuscleGroup.BICEPS, emptyList(), listOf(Equipment.BARBELL), emptyList(),
            Difficulty.BEGINNER, compound = false,
            prep = "Sujeta la barra EZ por las curvas interiores o exteriores, brazos estirados y codos a los lados del tronco.",
            steps = listOf(
                "Sube la barra doblando los codos sin moverlos de su sitio.",
                "Para cuando los antebrazos estén casi verticales.",
                "Baja la barra con control hasta estirar los brazos.",
            ),
            breathing = "Espira al subir e inspira al bajar.",
            mistakes = listOf(
                "Echar el cuerpo hacia atrás al final de la serie.",
                "Doblar las muñecas hacia dentro al subir.",
            ),
        ),
        ex(
            "biceps_hammer_curl", "Curl martillo", listOf("Hammer curl", "Curl neutro"),
            MuscleGroup.BICEPS, emptyList(), listOf(Equipment.DUMBBELL), emptyList(),
            Difficulty.BEGINNER, compound = false,
            prep = "De pie con una mancuerna en cada mano y las palmas mirándose, como si sujetaras martillos.",
            steps = listOf(
                "Sube las mancuernas sin girar la muñeca, con el pulgar hacia arriba.",
                "Llega hasta la altura del hombro con el codo fijo.",
                "Baja despacio a la posición inicial.",
            ),
            breathing = "Espira al subir e inspira al bajar.",
            mistakes = listOf(
                "Cruzar la mancuerna por delante del cuerpo en lugar de subirla recta.",
                "Dejar caer el peso en la bajada.",
            ),
        ),
        ex(
            "biceps_cable_curl", "Curl en polea", listOf("Curl en polea baja", "Cable curl"),
            MuscleGroup.BICEPS, emptyList(), listOf(Equipment.CABLE), emptyList(),
            Difficulty.BEGINNER, compound = false,
            prep = "Polea en la posición baja con barra recta o cuerda. Colócate cerca, de pie, con los codos pegados al cuerpo.",
            steps = listOf(
                "Dobla los codos llevando el agarre hacia el pecho.",
                "Aprieta arriba sin adelantar los codos.",
                "Baja manteniendo la tensión del cable hasta estirar los brazos.",
            ),
            breathing = "Espira al subir e inspira al bajar.",
            mistakes = listOf(
                "Ponerse tan lejos de la polea que el cable tira del cuerpo hacia delante.",
                "Inclinarse hacia atrás para terminar la repetición.",
            ),
        ),
        ex(
            "biceps_preacher_curl", "Curl predicador", listOf("Curl en banco Scott", "Curl Scott", "Preacher curl"),
            MuscleGroup.BICEPS, emptyList(), listOf(Equipment.BARBELL), listOf(Equipment.BENCH),
            Difficulty.INTERMEDIATE, compound = false,
            prep = "Ajusta el asiento para que las axilas descansen sobre el borde del atril y la parte trasera de los brazos quede apoyada.",
            steps = listOf(
                "Con los brazos casi estirados, sube la barra doblando los codos.",
                "Para antes de que los antebrazos queden verticales.",
                "Baja lentamente sin bloquear el codo de golpe abajo.",
            ),
            breathing = "Espira al subir e inspira al bajar.",
            mistakes = listOf(
                "Dejar caer la barra en el último tramo, donde el codo es más vulnerable.",
                "Levantar los brazos del atril para ayudarse con el hombro.",
            ),
        ),
        // ---------------- Tríceps ----------------
        ex(
            "triceps_rope_pushdown", "Extensión en polea con cuerda", listOf("Jalón de tríceps con cuerda", "Rope pushdown", "Tríceps en polea"),
            MuscleGroup.TRICEPS, emptyList(), listOf(Equipment.CABLE), emptyList(),
            Difficulty.BEGINNER, compound = false,
            prep = "Polea alta con cuerda. De pie, ligeramente inclinado, codos pegados al cuerpo y antebrazos paralelos al suelo.",
            steps = listOf(
                "Empuja la cuerda hacia abajo hasta estirar los brazos.",
                "Al final separa un poco los extremos de la cuerda hacia los lados.",
                "Sube despacio hasta que los antebrazos vuelvan a quedar horizontales.",
            ),
            breathing = "Espira al empujar e inspira al subir.",
            mistakes = listOf(
                "Mover los codos hacia delante y atrás durante el recorrido.",
                "Echar el peso del cuerpo encima de la cuerda.",
            ),
        ),
        ex(
            "triceps_bar_pushdown", "Extensión en polea con barra", listOf("Jalón de tríceps con barra", "Pushdown con barra", "Press francés en polea"),
            MuscleGroup.TRICEPS, emptyList(), listOf(Equipment.CABLE), emptyList(),
            Difficulty.BEGINNER, compound = false,
            prep = "Polea alta con barra recta o en V. Agarre prono a la anchura de los hombros y codos junto a las costillas.",
            steps = listOf(
                "Empuja la barra hacia los muslos estirando los codos.",
                "Mantén las muñecas firmes, alineadas con el antebrazo.",
                "Deja subir la barra con control hasta la altura del pecho bajo.",
            ),
            breathing = "Espira al empujar e inspira al subir.",
            mistakes = listOf(
                "Doblar las muñecas hacia atrás al empujar.",
                "Subir la barra tanto que los codos se despegan del cuerpo.",
            ),
        ),
        ex(
            "triceps_cable_overhead_extension", "Extensión por encima de la cabeza en polea", listOf("Extensión overhead", "Tríceps tras nuca en polea", "Overhead extension"),
            MuscleGroup.TRICEPS, emptyList(), listOf(Equipment.CABLE), emptyList(),
            Difficulty.INTERMEDIATE, compound = false,
            prep = "Cuerda en la polea baja o media. De espaldas a la máquina, da un paso al frente con la cuerda detrás de la cabeza y los codos apuntando hacia delante.",
            steps = listOf(
                "Estira los codos llevando las manos por encima y delante de la cabeza.",
                "Mantén los codos cerca de la cabeza sin abrirlos.",
                "Vuelve doblando los codos hasta notar estiramiento detrás del brazo.",
            ),
            breathing = "Espira al estirar e inspira al volver.",
            mistakes = listOf(
                "Arquear la zona lumbar para compensar.",
                "Abrir los codos hacia fuera, convirtiéndolo en un empuje de hombro.",
            ),
        ),
        ex(
            "triceps_ez_skull_crusher", "Extensión tumbado con barra EZ", listOf("Press francés", "Rompecráneos", "Skull crusher"),
            MuscleGroup.TRICEPS, emptyList(), listOf(Equipment.BARBELL), listOf(Equipment.BENCH),
            Difficulty.INTERMEDIATE, compound = false,
            prep = "Túmbate en un banco plano con la barra EZ sobre el pecho y los brazos estirados, ligeramente inclinados hacia la cabeza.",
            steps = listOf(
                "Dobla solo los codos bajando la barra hacia la frente o justo detrás de la cabeza.",
                "Mantén los brazos quietos apuntando hacia el techo.",
                "Estira los codos para volver a la posición inicial.",
            ),
            breathing = "Inspira al bajar y espira al estirar.",
            mistakes = listOf(
                "Bajar la barra deprisa hacia la cara.",
                "Mover los hombros y convertirlo en un press.",
                "Abrir los codos hacia los lados.",
            ),
        ),
        ex(
            "triceps_one_arm_dumbbell_extension", "Extensión unilateral con mancuerna", listOf("Extensión a una mano", "Tríceps con mancuerna tras nuca"),
            MuscleGroup.TRICEPS, emptyList(), listOf(Equipment.DUMBBELL), listOf(Equipment.BENCH),
            Difficulty.BEGINNER, compound = false,
            prep = "Sentado con la espalda recta, sujeta una mancuerna con una mano y súbela por encima de la cabeza. La otra mano puede sujetar el codo de trabajo.",
            steps = listOf(
                "Dobla el codo bajando la mancuerna por detrás de la cabeza.",
                "Mantén el codo apuntando al techo.",
                "Estira el brazo hasta arriba sin bloquear bruscamente.",
                "Cambia de brazo al terminar la serie.",
            ),
            breathing = "Inspira al bajar y espira al subir.",
            mistakes = listOf(
                "Dejar que el codo se abra hacia el lateral.",
                "Inclinar el tronco hacia un lado para compensar.",
            ),
        ),
        // ---------------- Cuádriceps ----------------
        ex(
            "quads_goblet_squat", "Sentadilla goblet", listOf("Sentadilla copa", "Goblet squat", "Sentadilla con mancuerna"),
            MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES, MuscleGroup.CORE), listOf(Equipment.DUMBBELL), emptyList(),
            Difficulty.BEGINNER, compound = true,
            prep = "Sujeta una mancuerna vertical junto al pecho con ambas manos. Pies algo más abiertos que la cadera y puntas un poco hacia fuera.",
            steps = listOf(
                "Baja doblando cadera y rodillas a la vez, como para sentarte entre los talones.",
                "Deja que las rodillas sigan la dirección de las puntas de los pies.",
                "Baja hasta donde mantengas la espalda recta y los talones apoyados.",
                "Empuja el suelo para subir.",
            ),
            breathing = "Inspira antes de bajar y espira al subir.",
            mistakes = listOf(
                "Levantar los talones del suelo al bajar.",
                "Dejar que las rodillas se cierren hacia dentro al subir.",
            ),
        ),
        ex(
            "quads_barbell_back_squat", "Sentadilla con barra", listOf("Sentadilla trasera", "Back squat", "Sentadilla libre"),
            MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS, MuscleGroup.CORE), listOf(Equipment.BARBELL), emptyList(),
            Difficulty.ADVANCED, compound = true,
            prep = "En un rack con topes de seguridad, coloca la barra sobre la parte alta de la espalda, no sobre el cuello. Saca la barra y separa los pies a la anchura de los hombros.",
            steps = listOf(
                "Toma aire, tensa el abdomen y baja doblando cadera y rodillas.",
                "Mantén el pecho alto y el peso repartido en todo el pie.",
                "Baja hasta la profundidad que controles sin redondear la espalda.",
                "Sube empujando el suelo con la cadera y el pecho a la vez.",
            ),
            breathing = "Inspira y bloquea el aire antes de bajar; espira al terminar la subida.",
            mistakes = listOf(
                "Entrenar sin topes de seguridad ajustados.",
                "Subir primero la cadera y dejar el pecho atrás.",
                "Rodillas que se cierran hacia dentro.",
            ),
        ),
        ex(
            "quads_leg_press", "Prensa de piernas", listOf("Prensa", "Leg press", "Prensa inclinada"),
            MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES), listOf(Equipment.MACHINE), emptyList(),
            Difficulty.BEGINNER, compound = true,
            prep = "Siéntate con la espalda baja pegada al respaldo y los pies en el centro de la plataforma, a la anchura de la cadera.",
            steps = listOf(
                "Libera los seguros y baja la plataforma doblando las rodillas.",
                "Para antes de que la zona lumbar se despegue del respaldo.",
                "Empuja con todo el pie hasta casi estirar las piernas.",
                "Al terminar, vuelve a poner los seguros.",
            ),
            breathing = "Inspira al bajar y espira al empujar.",
            mistakes = listOf(
                "Bloquear las rodillas de golpe arriba.",
                "Bajar tanto que la pelvis se levanta del asiento.",
            ),
        ),
        ex(
            "quads_leg_extension", "Extensión de rodilla en máquina", listOf("Extensión de cuádriceps", "Leg extension", "Sillón de cuádriceps"),
            MuscleGroup.QUADS, emptyList(), listOf(Equipment.MACHINE), emptyList(),
            Difficulty.BEGINNER, compound = false,
            prep = "Ajusta el respaldo para que la rodilla quede alineada con el eje de la máquina y el rodillo apoye sobre el tobillo.",
            steps = listOf(
                "Estira las rodillas subiendo el rodillo.",
                "Aprieta el muslo un instante arriba.",
                "Baja despacio sin dejar que las placas choquen.",
            ),
            breathing = "Espira al subir e inspira al bajar.",
            mistakes = listOf(
                "Dar patadas rápidas con impulso.",
                "Levantar la cadera del asiento al final.",
            ),
        ),
        ex(
            "quads_split_squat", "Sentadilla dividida", listOf("Split squat", "Zancada estática", "Sentadilla búlgara (con pie elevado)"),
            MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES), listOf(Equipment.BODYWEIGHT), listOf(Equipment.DUMBBELL, Equipment.BENCH),
            Difficulty.INTERMEDIATE, compound = true,
            prep = "Da un paso largo: un pie delante y el otro detrás apoyado en la punta. Puedes sujetar mancuernas o apoyar el pie trasero en un banco para una variante más difícil.",
            steps = listOf(
                "Baja en vertical doblando las dos rodillas.",
                "Detente cuando la rodilla trasera quede cerca del suelo.",
                "Empuja con el pie delantero para subir.",
                "Termina las repeticiones y cambia de pierna.",
            ),
            breathing = "Inspira al bajar y espira al subir.",
            mistakes = listOf(
                "Colocar los pies en una misma línea, como sobre una cuerda, y perder el equilibrio.",
                "Empujar con la pierna de atrás en lugar de la delantera.",
            ),
        ),
        // ---------------- Isquiotibiales ----------------
        ex(
            "hamstrings_seated_leg_curl", "Curl femoral sentado", listOf("Curl de isquios sentado", "Seated leg curl"),
            MuscleGroup.HAMSTRINGS, emptyList(), listOf(Equipment.MACHINE), emptyList(),
            Difficulty.BEGINNER, compound = false,
            prep = "Ajusta la máquina para que la rodilla quede alineada con el eje, el rodillo bajo los tobillos y la sujeción firme sobre los muslos.",
            steps = listOf(
                "Dobla las rodillas llevando el rodillo hacia abajo y atrás.",
                "Aprieta la parte trasera del muslo al final.",
                "Vuelve despacio hasta casi estirar las piernas.",
            ),
            breathing = "Espira al doblar e inspira al volver.",
            mistakes = listOf(
                "Dejar la sujeción de los muslos floja, lo que levanta la cadera.",
                "Volver de golpe sin controlar el peso.",
            ),
        ),
        ex(
            "hamstrings_lying_leg_curl", "Curl femoral tumbado", listOf("Curl de isquios tumbado", "Lying leg curl", "Femoral tumbado"),
            MuscleGroup.HAMSTRINGS, listOf(MuscleGroup.CALVES), listOf(Equipment.MACHINE), emptyList(),
            Difficulty.BEGINNER, compound = false,
            prep = "Túmbate boca abajo con las rodillas justo por fuera del borde del banco y el rodillo sobre los talones. Sujeta las asas.",
            steps = listOf(
                "Dobla las rodillas acercando los talones a los glúteos.",
                "Mantén la cadera pegada al banco.",
                "Baja con control hasta casi estirar las piernas.",
            ),
            breathing = "Espira al subir e inspira al bajar.",
            mistakes = listOf(
                "Levantar la cadera del banco para completar el movimiento.",
                "Acortar el recorrido sin estirar las piernas abajo.",
            ),
        ),
        ex(
            "hamstrings_barbell_romanian_deadlift", "Peso muerto rumano con barra", listOf("Peso muerto rumano", "RDL", "Romanian deadlift"),
            MuscleGroup.HAMSTRINGS, listOf(MuscleGroup.GLUTES, MuscleGroup.BACK), listOf(Equipment.BARBELL), emptyList(),
            Difficulty.ADVANCED, compound = true,
            prep = "De pie con la barra en las manos delante de los muslos, pies a la anchura de la cadera y rodillas un poco dobladas.",
            steps = listOf(
                "Lleva la cadera hacia atrás inclinando el tronco con la espalda recta.",
                "Desliza la barra pegada a los muslos y luego a las espinillas.",
                "Baja hasta notar tensión en la parte trasera del muslo sin redondear la espalda.",
                "Vuelve a subir empujando la cadera hacia delante.",
            ),
            breathing = "Inspira y tensa el abdomen antes de bajar; espira al terminar de subir.",
            mistakes = listOf(
                "Redondear la espalda para llegar más abajo.",
                "Separar la barra del cuerpo.",
                "Doblar mucho las rodillas, convirtiéndolo en una sentadilla.",
            ),
        ),
        ex(
            "hamstrings_dumbbell_romanian_deadlift", "Peso muerto rumano con mancuernas", listOf("Rumano con mancuernas", "Dumbbell RDL"),
            MuscleGroup.HAMSTRINGS, listOf(MuscleGroup.GLUTES, MuscleGroup.BACK), listOf(Equipment.DUMBBELL), emptyList(),
            Difficulty.INTERMEDIATE, compound = true,
            prep = "De pie con una mancuerna en cada mano delante de los muslos y rodillas ligeramente flexionadas.",
            steps = listOf(
                "Empuja la cadera hacia atrás mientras las mancuernas bajan cerca de las piernas.",
                "Mantén la espalda recta y la mirada unos metros por delante.",
                "Detente al notar estiramiento detrás del muslo.",
                "Sube apretando los glúteos.",
            ),
            breathing = "Inspira al bajar y espira al subir.",
            mistakes = listOf(
                "Dejar que las mancuernas cuelguen lejos de las piernas.",
                "Mirar hacia arriba forzando el cuello.",
            ),
        ),
        ex(
            "hamstrings_slider_leg_curl", "Curl femoral con deslizadores", listOf("Curl nórdico con deslizadores", "Slider leg curl", "Curl femoral en el suelo"),
            MuscleGroup.HAMSTRINGS, listOf(MuscleGroup.GLUTES, MuscleGroup.CORE), listOf(Equipment.BODYWEIGHT), emptyList(),
            Difficulty.INTERMEDIATE, compound = false,
            prep = "Túmbate boca arriba con los talones sobre deslizadores o toallas en un suelo liso y las rodillas dobladas.",
            steps = listOf(
                "Eleva la cadera hasta alinear hombros, cadera y rodillas.",
                "Desliza los talones alejándolos sin dejar caer la cadera.",
                "Tira de los talones hacia los glúteos para volver.",
            ),
            breathing = "Inspira al alejar los pies y espira al acercarlos.",
            mistakes = listOf(
                "Bajar la cadera al suelo al estirar las piernas.",
                "Hacerlo sobre una superficie que frena o engancha los deslizadores.",
            ),
        ),
        // ---------------- Glúteos ----------------
        ex(
            "glutes_barbell_hip_thrust", "Hip thrust con barra", listOf("Empuje de cadera", "Hip thrust", "Puente de cadera con barra"),
            MuscleGroup.GLUTES, listOf(MuscleGroup.HAMSTRINGS), listOf(Equipment.BARBELL), listOf(Equipment.BENCH),
            Difficulty.INTERMEDIATE, compound = true,
            prep = "Siéntate en el suelo con la parte baja de los omóplatos contra un banco y la barra sobre la cadera, con almohadilla. Pies apoyados y rodillas dobladas.",
            steps = listOf(
                "Empuja con los talones elevando la cadera.",
                "Arriba, las rodillas forman unos 90° y el tronco queda en línea con los muslos.",
                "Aprieta los glúteos sin arquear la zona lumbar.",
                "Baja la cadera con control.",
            ),
            breathing = "Inspira abajo y espira al elevar la cadera.",
            mistakes = listOf(
                "Arquear la espalda en lugar de extender la cadera.",
                "Colocar los pies muy lejos y notar el trabajo solo en isquios.",
            ),
        ),
        ex(
            "glutes_bridge", "Puente de glúteos", listOf("Glute bridge", "Elevación de cadera", "Puente"),
            MuscleGroup.GLUTES, listOf(MuscleGroup.HAMSTRINGS), listOf(Equipment.BODYWEIGHT), emptyList(),
            Difficulty.BEGINNER, compound = false,
            prep = "Túmbate boca arriba con las rodillas dobladas, pies a la anchura de la cadera y brazos a los lados.",
            steps = listOf(
                "Aprieta los glúteos y eleva la cadera empujando con los talones.",
                "Sube hasta alinear hombros, cadera y rodillas.",
                "Mantén un segundo y baja vértebra a vértebra.",
            ),
            breathing = "Espira al subir e inspira al bajar.",
            mistakes = listOf(
                "Subir demasiado y cargar la zona lumbar.",
                "Dejar que las rodillas se abran o se cierren.",
            ),
        ),
        ex(
            "glutes_cable_kickback", "Patada en polea", listOf("Patada de glúteo", "Cable kickback", "Patada trasera"),
            MuscleGroup.GLUTES, listOf(MuscleGroup.HAMSTRINGS), listOf(Equipment.CABLE), emptyList(),
            Difficulty.BEGINNER, compound = false,
            prep = "Coloca la tobillera en la polea baja. De frente a la máquina, sujétate y apoya el peso en la pierna libre con la rodilla suave.",
            steps = listOf(
                "Lleva la pierna hacia atrás manteniendo la rodilla casi estirada.",
                "Para cuando la cadera esté totalmente extendida, sin girar la pelvis.",
                "Vuelve despacio con la pierna hacia delante.",
                "Completa la serie y cambia de pierna.",
            ),
            breathing = "Espira al llevar la pierna atrás e inspira al volver.",
            mistakes = listOf(
                "Arquear la zona lumbar para subir más la pierna.",
                "Girar la cadera abriendo la pierna hacia fuera.",
            ),
        ),
        ex(
            "glutes_machine_hip_abduction", "Abducción en máquina", listOf("Abductores en máquina", "Hip abduction", "Máquina de abductores"),
            MuscleGroup.GLUTES, emptyList(), listOf(Equipment.MACHINE), emptyList(),
            Difficulty.BEGINNER, compound = false,
            prep = "Siéntate con la espalda apoyada y las almohadillas por fuera de las rodillas. Ajusta la apertura inicial a un punto cómodo.",
            steps = listOf(
                "Separa las rodillas empujando las almohadillas hacia fuera.",
                "Mantén un instante en la apertura máxima cómoda.",
                "Cierra despacio sin que las placas choquen.",
            ),
            breathing = "Espira al abrir e inspira al cerrar.",
            mistakes = listOf(
                "Balancear el tronco hacia delante y atrás.",
                "Cerrar de golpe soltando la carga.",
            ),
        ),
        ex(
            "glutes_step_up", "Subida al banco", listOf("Step up", "Subida al cajón", "Subidas a banco"),
            MuscleGroup.GLUTES, listOf(MuscleGroup.QUADS), listOf(Equipment.BENCH), listOf(Equipment.DUMBBELL),
            Difficulty.INTERMEDIATE, compound = true,
            prep = "Ponte frente a un banco estable a la altura de la rodilla o algo menos. Apoya un pie entero encima; puedes sujetar mancuernas.",
            steps = listOf(
                "Empuja con el pie de arriba para subir el cuerpo.",
                "Termina de pie sobre el banco sin impulsarte con la pierna de abajo.",
                "Baja controlando con la misma pierna.",
                "Completa la serie y cambia de pierna.",
            ),
            breathing = "Espira al subir e inspira al bajar.",
            mistakes = listOf(
                "Impulsarse con el pie del suelo.",
                "Usar un banco inestable o demasiado alto.",
            ),
        ),
        // ---------------- Gemelos ----------------
        ex(
            "calves_standing_raise", "Elevaciones de talón de pie", listOf("Elevación de gemelos", "Calf raise", "Gemelos de pie"),
            MuscleGroup.CALVES, emptyList(), listOf(Equipment.BODYWEIGHT), listOf(Equipment.DUMBBELL),
            Difficulty.BEGINNER, compound = false,
            prep = "De pie con la parte delantera de los pies sobre un escalón y los talones en el aire. Sujétate a algo estable; puedes añadir mancuernas.",
            steps = listOf(
                "Sube de puntillas todo lo que puedas.",
                "Mantén un segundo arriba.",
                "Baja los talones por debajo del escalón hasta notar estiramiento.",
            ),
            breathing = "Espira al subir e inspira al bajar.",
            mistakes = listOf(
                "Rebotar abajo en lugar de bajar con control.",
                "Doblar las rodillas para ayudarse.",
            ),
        ),
        ex(
            "calves_seated_machine_raise", "Elevaciones sentado en máquina", listOf("Gemelo sentado", "Seated calf raise", "Sóleo en máquina"),
            MuscleGroup.CALVES, emptyList(), listOf(Equipment.MACHINE), emptyList(),
            Difficulty.BEGINNER, compound = false,
            prep = "Siéntate con la almohadilla sobre los muslos, cerca de las rodillas, y la parte delantera de los pies en la plataforma.",
            steps = listOf(
                "Quita el seguro y deja bajar los talones hasta notar estiramiento.",
                "Sube los talones todo lo posible.",
                "Mantén un instante y baja despacio.",
            ),
            breathing = "Espira al subir e inspira al bajar.",
            mistakes = listOf(
                "Mover solo unos centímetros sin estirar abajo.",
                "Empujar con las manos sobre la almohadilla.",
            ),
        ),
        ex(
            "calves_leg_press_raise", "Elevaciones en prensa", listOf("Gemelos en prensa", "Calf press", "Prensa de gemelos"),
            MuscleGroup.CALVES, emptyList(), listOf(Equipment.MACHINE), emptyList(),
            Difficulty.INTERMEDIATE, compound = false,
            prep = "En la prensa, apoya solo la parte delantera de los pies en el borde bajo de la plataforma con las piernas casi estiradas. Mantén los seguros puestos mientras te colocas.",
            steps = listOf(
                "Empuja la plataforma con las puntas extendiendo los tobillos.",
                "Deja volver la plataforma doblando los tobillos sin doblar las rodillas.",
                "Repite con un ritmo constante y controlado.",
            ),
            breathing = "Espira al empujar e inspira al volver.",
            mistakes = listOf(
                "Apoyar los pies tan abajo que pueden resbalar de la plataforma.",
                "Doblar las rodillas y mover la plataforma con los cuádriceps.",
            ),
        ),
        ex(
            "calves_single_leg_bodyweight_raise", "Elevación unilateral con peso corporal", listOf("Gemelo a una pierna", "Single leg calf raise"),
            MuscleGroup.CALVES, emptyList(), listOf(Equipment.BODYWEIGHT), emptyList(),
            Difficulty.BEGINNER, compound = false,
            prep = "Sobre un escalón, apoya la parte delantera de un pie y recoge el otro detrás del tobillo. Sujétate a la pared o a una barandilla.",
            steps = listOf(
                "Sube de puntillas con una sola pierna.",
                "Baja el talón por debajo del escalón con control.",
                "Completa la serie y cambia de pierna.",
            ),
            breathing = "Espira al subir e inspira al bajar.",
            mistakes = listOf(
                "Tirar con los brazos de la barandilla para subir.",
                "Girar el pie hacia fuera durante el recorrido.",
            ),
        ),
        ex(
            "calves_standing_machine_raise", "Elevaciones de pie en máquina", listOf("Gemelo de pie en máquina", "Standing calf machine"),
            MuscleGroup.CALVES, emptyList(), listOf(Equipment.MACHINE), emptyList(),
            Difficulty.BEGINNER, compound = false,
            prep = "Coloca los hombros bajo las almohadillas y la parte delantera de los pies en la plataforma, con las piernas estiradas.",
            steps = listOf(
                "Sube de puntillas empujando las almohadillas.",
                "Mantén un segundo en la posición más alta.",
                "Baja los talones despacio hasta el estiramiento.",
            ),
            breathing = "Espira al subir e inspira al bajar.",
            mistakes = listOf(
                "Doblar las rodillas en cada repetición.",
                "Encoger los hombros bajo la almohadilla.",
            ),
        ),
        // ---------------- Abdomen / core ----------------
        ex(
            "core_mat_crunch", "Crunch en colchoneta", listOf("Encogimiento abdominal", "Crunch", "Abdominales"),
            MuscleGroup.CORE, emptyList(), listOf(Equipment.BODYWEIGHT), emptyList(),
            Difficulty.BEGINNER, compound = false,
            prep = "Túmbate boca arriba sobre una colchoneta con las rodillas dobladas y las manos sobre el pecho o junto a las sienes.",
            steps = listOf(
                "Despega la cabeza y los omóplatos acercando las costillas a la cadera.",
                "Mantén la zona lumbar apoyada en el suelo.",
                "Baja despacio sin dejar caer la cabeza.",
            ),
            breathing = "Espira al subir e inspira al bajar.",
            mistakes = listOf(
                "Tirar de la cabeza con las manos.",
                "Subir el tronco entero hasta sentarse.",
            ),
        ),
        ex(
            "core_cable_crunch", "Crunch en polea", listOf("Crunch de rodillas en polea", "Cable crunch", "Abdominal en polea"),
            MuscleGroup.CORE, emptyList(), listOf(Equipment.CABLE), emptyList(),
            Difficulty.INTERMEDIATE, compound = false,
            prep = "Cuerda en la polea alta. Arrodíllate frente a la máquina y sujeta la cuerda junto a la cabeza.",
            steps = listOf(
                "Flexiona el tronco llevando los codos hacia los muslos.",
                "Redondea la columna desde las costillas sin mover la cadera.",
                "Vuelve despacio hasta la posición inicial.",
            ),
            breathing = "Espira al bajar y encoger e inspira al volver.",
            mistakes = listOf(
                "Sentarse sobre los talones y mover la cadera en lugar del tronco.",
                "Tirar de la cuerda con los brazos.",
            ),
        ),
        ex(
            "core_machine_crunch", "Crunch en máquina", listOf("Máquina de abdominales", "Ab crunch machine"),
            MuscleGroup.CORE, emptyList(), listOf(Equipment.MACHINE), emptyList(),
            Difficulty.BEGINNER, compound = false,
            prep = "Ajusta el asiento para que las almohadillas o asas queden a la altura del pecho y siéntate con la espalda apoyada.",
            steps = listOf(
                "Flexiona el tronco hacia delante acercando el pecho a la cadera.",
                "Aprieta el abdomen un instante.",
                "Vuelve controlando el peso.",
            ),
            breathing = "Espira al flexionar e inspira al volver.",
            mistakes = listOf(
                "Empujar con los brazos o las piernas en lugar del abdomen.",
                "Usar tanto peso que el movimiento se vuelve brusco.",
            ),
        ),
        ex(
            "core_reverse_crunch", "Crunch inverso", listOf("Elevación de cadera tumbado", "Reverse crunch"),
            MuscleGroup.CORE, emptyList(), listOf(Equipment.BODYWEIGHT), emptyList(),
            Difficulty.INTERMEDIATE, compound = false,
            prep = "Túmbate boca arriba con los brazos a los lados, caderas y rodillas dobladas a 90° y las piernas en el aire.",
            steps = listOf(
                "Acerca las rodillas al pecho despegando la cadera del suelo.",
                "Usa el abdomen, no el impulso de las piernas.",
                "Baja la cadera despacio hasta volver a 90°.",
            ),
            breathing = "Espira al elevar la cadera e inspira al bajar.",
            mistakes = listOf(
                "Balancear las piernas para lanzar la cadera.",
                "Estirar las piernas al bajar y arquear la zona lumbar.",
            ),
        ),
        ex(
            "core_stability_ball_crunch", "Crunch en fitball", listOf("Crunch en pelota", "Abdominales en fitball", "Swiss ball crunch"),
            MuscleGroup.CORE, emptyList(), listOf(Equipment.BODYWEIGHT), emptyList(),
            Difficulty.BEGINNER, compound = false,
            prep = "Siéntate en una pelota de ejercicio y camina hacia delante hasta apoyar la zona lumbar en ella, pies separados en el suelo.",
            steps = listOf(
                "Deja que el tronco se extienda un poco sobre la pelota.",
                "Flexiona el tronco hacia delante acercando las costillas a la cadera.",
                "Vuelve despacio manteniendo la pelota quieta.",
            ),
            breathing = "Espira al subir e inspira al volver.",
            mistakes = listOf(
                "Juntar demasiado los pies y perder el equilibrio.",
                "Mover la pelota hacia delante y atrás con las piernas.",
            ),
        ),
    )

    private val index: Map<String, CatalogExercise> = all.associateBy { it.id }

    @Suppress("LongParameterList")
    private fun ex(
        id: String,
        name: String,
        aliases: List<String>,
        primary: MuscleGroup,
        secondary: List<MuscleGroup>,
        equipment: List<Equipment>,
        accessories: List<Equipment>,
        difficulty: Difficulty,
        compound: Boolean,
        prep: String,
        steps: List<String>,
        breathing: String,
        mistakes: List<String>,
    ) = CatalogExercise(id, name, aliases, primary, secondary, equipment, accessories, difficulty, compound, prep, steps, breathing, mistakes)
}
