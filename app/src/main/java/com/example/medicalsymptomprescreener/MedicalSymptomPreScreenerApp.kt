package com.example.medicalsymptomprescreener

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for the Medical Symptom Pre-Screener.
 *
 * Annotated with [@HiltAndroidApp] to trigger Hilt's code generation and initialize
 * the application-level dependency injection component. This class must be declared
 * in [AndroidManifest.xml] via `android:name=".MedicalSymptomPreScreenerApp"`.
 *
 * S: Single Responsibility — initializes Hilt DI at application startup.
 */
@HiltAndroidApp
class MedicalSymptomPreScreenerApp : Application()
