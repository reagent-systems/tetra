# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Build and Test
```bash
# Navigate to Tetra directory for all gradle commands
cd Tetra

# Build debug APK
./gradlew assembleDebug

# Build release APK  
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Install debug APK to connected device
./gradlew installDebug
# or via ADB after build
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Development Environment
- **Target SDK**: 35 (Android 15)
- **Min SDK**: 32 (Android 12L)
- **Java Version**: 11
- **Kotlin**: 2.0.0 with Compose plugin
- **Build System**: Gradle with Version Catalogs (libs.versions.toml)

## Architecture Overview

This is an AI-powered Android automation agent built with Jetpack Compose that uses OpenAI's GPT models to understand screen content and perform autonomous actions via Android Accessibility Services.

### Core Components

#### Agent Execution (`/agentcore/`)
- **AgentOrchestrator.kt**: Main execution engine that coordinates agent operations
  - Manages conversation history with OpenAI
  - Implements step-by-step task execution (max 15 steps)
  - Handles loop detection and error recovery
  - Orchestrates tool calls for Android actions

- **LLMClient.kt**: OpenAI API integration with function calling
  - Uses GPT-4o model for decision making
  - Defines tool schema for Android interactions (press, text input, swipe, navigation)
  - Comprehensive error tracking for API calls

- **AgentActions.kt**: Low-level Android UI interaction layer
  - Executes physical gestures via Accessibility Service
  - Screen parsing and element identification
  - Coordinate-based interaction system

#### AI Decision Making (`/agentcore/metacognition/`)
- **MetaCognition.kt**: Task planning and progress reflection
- **LoopDetector.kt**: Detects repetitive action patterns
- **Prompts.kt**: System prompts and instruction templates

#### Accessibility Layer (`/accessibility/`)
- **BoundingBoxAccessibilityService.kt**: Core accessibility service
- **AccessibilityNodeHandler.kt**: UI hierarchy parsing
- **OverlayManager.kt**: Debug overlays and bounding boxes
- **FloatingButtonManager.kt**: Floating UI controls

#### User Interface (`/ui/`)
- **HomeScreen.kt**: Main agent control interface
- **DebugScreen.kt**: Development and troubleshooting tools
- **SettingsScreen.kt**: Configuration management
- Built with Jetpack Compose and Material 3

#### Monitoring (`/sentry/`)
- **SentryManager.kt**: Error tracking and performance monitoring
- **ApiErrorTracker.kt**: OpenAI API error analysis
- **AgentErrorTracker.kt**: Agent execution metrics

### Key Architectural Patterns

1. **Conversation-based AI**: Agent maintains message history with OpenAI, building context over multiple interaction steps

2. **Tool-based Function Calling**: OpenAI responds with structured function calls that map to Android actions (simulate_press, set_text, swipe, etc.)

3. **Screen-to-JSON Parsing**: Accessibility service converts UI hierarchy to JSON for AI consumption

4. **Coordinate-based Interaction**: All UI interactions use screen coordinates from parsed elements

5. **Multi-layered Error Handling**: Comprehensive error tracking at API, agent execution, and UI interaction levels

## Development Guidelines

### Working with Agent Logic
- Agent execution is limited to 15 steps maximum
- Loop detection triggers after 2 identical actions in 3 recent actions
- Screen JSON is refreshed before each agent step
- Text input always requires: press → wait → set_text sequence

### OpenAI Integration
- All API calls go through LLMClient with comprehensive error tracking
- Function calling schema defines available Android actions
- Conversation history is maintained but screen JSON is refreshed each step
- Rate limiting and quota errors are tracked via Sentry

### Accessibility Service
- Requires manual user enablement in Android Settings
- Parses UI hierarchy into JSON with coordinates, text, and element types
- Supports both debug overlays and production interaction
- All UI interactions run on main thread via Handler

### Testing
- Unit tests in `/test/` directory
- Instrumented tests for accessibility service functionality
- Manual testing requires physical device with accessibility permissions
- Test various OpenAI API scenarios and error conditions

### Configuration
- OpenAI API key stored in SharedPreferences
- Sentry configured for "reagent-systems" organization
- Floating UI and debug features togglable via settings
- Comprehensive logging via LogManager utility

## Project Structure Notes

- Main source: `Tetra/app/src/main/java/com/example/simple_agent_android/`
- Resources: `Tetra/app/src/main/res/`
- Build outputs: `Tetra/app/build/outputs/apk/`
- The project root contains multiple directories; **Tetra/** is the active Android project