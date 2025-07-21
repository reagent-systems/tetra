# Tetra (Simple Agent Android)

An AI-powered Android automation agent that can understand screen content and perform actions autonomously using OpenAI's GPT models and Android's Accessibility Services.

## 🚀 Features

### Core Functionality
- **AI-Powered Screen Analysis**: Uses OpenAI GPT models to understand Android UI elements and screen context
- **Autonomous Actions**: Performs taps, text input, swipes, and navigation based on natural language instructions
- **Voice Input Support**: Convert speech to text for hands-free agent commands
- **Real-time Screen Parsing**: Analyzes UI hierarchy and identifies interactable elements
- **Floating UI Controls**: Overlay interface for quick agent access without leaving current app

### Advanced Capabilities
- **Loop Detection**: Intelligent detection and recovery from repetitive action patterns
- **Context-Aware Decision Making**: Maintains task context and progress tracking
- **Metacognitive Reflection**: Self-assessment of progress and decision to continue or stop
- **Error Recovery**: Robust handling of UI state changes and unexpected scenarios
- **Performance Monitoring**: Built-in analytics and performance tracking

### Monitoring & Debugging
- **Comprehensive Sentry Integration**: Real-time error tracking and performance monitoring
- **API Error Tracking**: Detailed OpenAI API call monitoring with rate limit detection
- **Agent Performance Analytics**: Step-by-step execution tracking and success metrics
- **Debug Interface**: Built-in debugging tools for development and troubleshooting

## 📋 Prerequisites

- **Android Studio** (Arctic Fox or later)
- **Android Device/Emulator** (API level 24+)
- **OpenAI API Key** with GPT-4 access
- **Sentry Account** (optional, for error tracking)

## 🛠️ Installation

### 1. Clone the Repository
```bash
git clone https://github.com/reagent-systems/Simple-Agent-Android.git
cd SimpleAgentAndroid2
```

### 2. Build and Install
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 🎯 Quick Start

### 1. Enable Accessibility Service
1. Open the app
2. Tap "Enable Accessibility Service"
3. Navigate to Settings → Accessibility → Simple Agent Android
4. Toggle the service ON

### 2. Basic Usage
1. Enter a natural language instruction (e.g., "Open Gmail and check my inbox")
2. Optionally use voice input by tapping the microphone
3. Tap "Start Agent" to begin autonomous execution
4. Monitor progress in real-time through the output display

### 3. Floating UI (Optional)
- Enable "Floating UI" for quick access while using other apps
- The floating button appears as an overlay on all screens
- Tap to quickly start/stop the agent without switching apps

## 🏗️ Architecture

### Core Components

#### Agent Orchestrator (`AgentOrchestrator.kt`)
- Main execution engine that coordinates all agent operations
- Manages conversation history and OpenAI API interactions
- Implements step-by-step task execution with error handling
- Integrates loop detection and metacognitive decision making

#### LLM Client (`LLMClient.kt`)
- Handles all OpenAI API communications
- Implements tool calling for Android actions
- Manages request/response formatting and error handling
- Supports function calling for structured agent actions

#### Screen Analysis (`ScreenAnalyzer.kt`)
- Parses Android UI hierarchy using Accessibility Services
- Identifies and prioritizes interactable elements
- Extracts text content, buttons, and input fields
- Provides structured screen context to the AI

#### Agent Actions (`AgentActions.kt`)
- Executes physical interactions with Android UI
- Supports tap, text input, swipe, and navigation gestures
- Implements element waiting and screen state verification
- Handles accessibility service integration

### AI & Decision Making

#### Metacognition (`MetaCognition.kt`)
- **Task Planning**: Breaks down complex instructions into steps
- **Progress Reflection**: Analyzes each action's effectiveness
- **Stopping Decisions**: Determines when objectives are completed
- **Error Analysis**: Identifies and responds to failure patterns

#### Loop Detection (`LoopDetector.kt`)
- **Exact Repetition**: Detects identical repeated actions
- **Semantic Loops**: Identifies functionally similar action patterns
- **Progress Stagnation**: Monitors for lack of meaningful progress
- **Recovery Strategies**: Implements intelligent loop breaking

### Monitoring & Analytics

#### Sentry Integration (`/sentry/`)
- **SentryManager**: Core error tracking and performance monitoring
- **ApiErrorTracker**: Specialized OpenAI API error analysis
- **AgentErrorTracker**: Agent-specific performance and error tracking
- **SentryExtensions**: Helper functions for easy integration

## 🔧 Configuration

### Agent Behavior
```kotlin
// Maximum steps before automatic termination
private const val MAX_STEPS = 10

// Loop detection sensitivity
private const val MAX_SIMILAR_ACTIONS = 2
private const val MAX_NO_PROGRESS_STEPS = 3
```

### OpenAI Settings
```kotlin
// Model configuration
private const val MODEL = "gpt-4-turbo-preview"
private const val API_URL = "https://api.openai.com/v1/chat/completions"
```

### Performance Thresholds
```kotlin
// Slow operation detection (milliseconds)
private const val SLOW_OPERATION_THRESHOLD = 5000L

// API response time monitoring
private const val SLOW_API_THRESHOLD = 10000L
```

## 🛡️ Permissions

The app requires the following permissions:

### Required Permissions
- `SYSTEM_ALERT_WINDOW`: For floating UI overlay
- `RECORD_AUDIO`: For voice input functionality
- `INTERNET`: For OpenAI API communication
- `ACCESS_NETWORK_STATE`: For network status monitoring

### Accessibility Service
- **Accessibility Service**: Required for screen reading and UI interaction
- **Usage**: Reads screen content and performs automated actions
- **Privacy**: All screen data is processed locally and only high-level context is sent to OpenAI

## 🔍 Debugging

### Built-in Debug Tools
1. **Debug Screen**: Access via navigation drawer
2. **Real-time Logs**: View agent execution steps
3. **Screen Analysis**: Inspect parsed UI elements
4. **API Monitoring**: Track OpenAI API calls and responses

### Common Issues

#### "Accessibility Service Not Enabled"
- **Solution**: Enable the service in Android Settings → Accessibility

#### "OpenAI API Error"
- **Check**: API key validity and account credits
- **Monitor**: Rate limiting and quota usage in Sentry

#### "Agent Stuck in Loop"
- **Automatic**: Loop detection will trigger recovery
- **Manual**: Stop and restart with refined instructions

## 📊 Monitoring Dashboard

### Sentry Analytics
- **Error Tracking**: Real-time error monitoring and alerting
- **Performance**: API response times and agent execution metrics
- **Usage Patterns**: Most common tasks and success rates
- **Failure Analysis**: Detailed error context and stack traces

### Key Metrics
- **Task Success Rate**: Percentage of successfully completed instructions
- **Average Execution Time**: Time from start to task completion
- **API Call Efficiency**: Requests per successful task completion
- **Loop Detection Rate**: Frequency of loop detection and recovery

## 🤝 Contributing

### Development Setup
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Add tests for new functionality
5. Commit changes (`git commit -m 'Add amazing feature'`)
6. Push to branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comprehensive comments for complex logic
- Include error handling for all external calls

### Testing
- Test on multiple Android versions and devices
- Verify accessibility service functionality
- Test with various OpenAI API scenarios
- Validate error handling and recovery mechanisms

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **OpenAI** for providing the GPT models that power the AI decision making
- **Sentry** for comprehensive error tracking and performance monitoring
- **Android Accessibility Services** for enabling UI automation capabilities
- **Kotlin Coroutines** for handling asynchronous operations efficiently

## 📞 Support

### Documentation
- **Wiki**: Detailed guides and tutorials
- **API Reference**: Complete function and class documentation
- **Examples**: Sample use cases and implementation patterns

### Community
- **Issues**: Report bugs and request features on GitHub
- **Discussions**: Join community discussions and share experiences
- **Discord**: Real-time chat with other developers and users

[Join our discord!](https://discord.reagent-systems.com/)

---

**⚠️ Important**: This app requires accessibility permissions to function. It reads screen content to understand context but only sends high-level descriptions to OpenAI, never raw screen data or sensitive information. 
