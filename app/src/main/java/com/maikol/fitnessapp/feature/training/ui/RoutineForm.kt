package com.maikol.fitnessapp.feature.training.ui

import android.os.Bundle
import com.maikol.fitnessapp.core.preferences.UnitSystem
import com.maikol.fitnessapp.feature.training.data.LoadText
import com.maikol.fitnessapp.feature.training.data.Parsed
import com.maikol.fitnessapp.feature.training.data.PlannedSet
import com.maikol.fitnessapp.feature.training.data.Routine
import com.maikol.fitnessapp.feature.training.data.RoutineExercise
import com.maikol.fitnessapp.feature.training.data.parseReps
import com.maikol.fitnessapp.feature.training.data.parseRestSeconds

/** Formulario del editor: texto tal como lo escribe la persona (incluidos valores aún no válidos). */
data class SetForm(val id: String, val reps: String, val load: LoadText, val rest: String, val warmup: Boolean)

data class ExerciseForm(val id: String, val key: String, val name: String, val notes: String, val sets: List<SetForm>)

data class RoutineForm(val id: String, val name: String, val notes: String, val exercises: List<ExerciseForm>)

/** Errores por campo (IDs de ejercicio/serie afectados). */
data class FormErrors(
    val name: Boolean = false,
    val noExercises: Boolean = false,
    val exerciseName: Set<String> = emptySet(),
    val noSets: Set<String> = emptySet(),
    val reps: Set<String> = emptySet(),
    val load: Set<String> = emptySet(),
    val rest: Set<String> = emptySet(),
) {
    val isEmpty: Boolean
        get() = !name && !noExercises && exerciseName.isEmpty() && noSets.isEmpty() && reps.isEmpty() && load.isEmpty() && rest.isEmpty()
}

sealed interface FormResult {
    data class Valid(val routine: Routine) : FormResult
    data class Invalid(val errors: FormErrors) : FormResult
}

/**
 * Convierte el formulario en una rutina ejecutable o devuelve todos sus errores. Nunca produce una rutina
 * rota: nombre, ≥1 ejercicio con nombre y ≥1 serie, reps 1–999, carga ≥0 y descanso 0–3600 s.
 */
fun RoutineForm.validate(unit: UnitSystem): FormResult {
    val exerciseName = mutableSetOf<String>()
    val noSets = mutableSetOf<String>()
    val reps = mutableSetOf<String>()
    val load = mutableSetOf<String>()
    val rest = mutableSetOf<String>()
    val exercises = exercises.map { e ->
        if (e.name.isBlank()) exerciseName += e.id
        if (e.sets.isEmpty()) noSets += e.id
        RoutineExercise(
            id = e.id, key = e.key, name = e.name.trim(), notes = e.notes.trim(),
            sets = e.sets.mapNotNull { s ->
                val r = parseReps(s.reps)
                val kg = s.load.resolveKg(unit)
                val restSeconds = parseRestSeconds(s.rest)
                if (r == null) reps += s.id
                if (kg is Parsed.Invalid) load += s.id
                if (restSeconds == null) rest += s.id
                if (r == null || kg is Parsed.Invalid || restSeconds == null) null
                else PlannedSet(s.id, r, (kg as? Parsed.Value)?.value?.takeIf { it > 0.0 }, restSeconds, s.warmup)
            },
        )
    }
    val errors = FormErrors(
        name = name.isBlank(),
        noExercises = exercises.isEmpty(),
        exerciseName = exerciseName, noSets = noSets, reps = reps, load = load, rest = rest,
    )
    return if (errors.isEmpty) FormResult.Valid(Routine(id, name.trim(), notes.trim(), archived = false, exercises = exercises))
    else FormResult.Invalid(errors)
}

fun Routine.toForm(unit: UnitSystem) = RoutineForm(
    id = id,
    name = name,
    notes = notes,
    exercises = exercises.map { e ->
        ExerciseForm(
            id = e.id, key = e.key, name = e.name, notes = e.notes,
            sets = e.sets.map { s -> SetForm(s.id, s.reps.toString(), LoadText.of(s.loadKg, unit), s.restSeconds.toString(), s.warmup) },
        )
    },
)

// ---- Conservación en SavedStateHandle (rotación y recreación del proceso por el sistema) ----

fun RoutineForm.toBundle(): Bundle = Bundle().apply {
    putString("id", id)
    putString("name", name)
    putString("notes", notes)
    putParcelableArrayList(
        "exercises",
        ArrayList(
            exercises.map { e ->
                Bundle().apply {
                    putString("id", e.id)
                    putString("key", e.key)
                    putString("name", e.name)
                    putString("notes", e.notes)
                    putParcelableArrayList(
                        "sets",
                        ArrayList(
                            e.sets.map { s ->
                                Bundle().apply {
                                    putString("id", s.id)
                                    putString("reps", s.reps)
                                    putString("loadText", s.load.text)
                                    putString("loadBaseText", s.load.baseText)
                                    s.load.baseKg?.let { putDouble("loadBaseKg", it) }
                                    putString("rest", s.rest)
                                    putBoolean("warmup", s.warmup)
                                }
                            },
                        ),
                    )
                }
            },
        ),
    )
}

@Suppress("DEPRECATION")
fun Bundle.toRoutineForm(): RoutineForm = RoutineForm(
    id = getString("id").orEmpty(),
    name = getString("name").orEmpty(),
    notes = getString("notes").orEmpty(),
    exercises = getParcelableArrayList<Bundle>("exercises").orEmpty().map { e ->
        ExerciseForm(
            id = e.getString("id").orEmpty(),
            key = e.getString("key").orEmpty(),
            name = e.getString("name").orEmpty(),
            notes = e.getString("notes").orEmpty(),
            sets = e.getParcelableArrayList<Bundle>("sets").orEmpty().map { s ->
                SetForm(
                    id = s.getString("id").orEmpty(),
                    reps = s.getString("reps").orEmpty(),
                    load = LoadText(
                        text = s.getString("loadText").orEmpty(),
                        baseKg = if (s.containsKey("loadBaseKg")) s.getDouble("loadBaseKg") else null,
                        baseText = s.getString("loadBaseText").orEmpty(),
                    ),
                    rest = s.getString("rest").orEmpty(),
                    warmup = s.getBoolean("warmup"),
                )
            },
        )
    },
)
