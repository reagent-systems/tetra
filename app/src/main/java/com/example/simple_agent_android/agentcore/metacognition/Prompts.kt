package com.example.simple_agent_android.agentcore.metacognition

object Prompts {
    val systemPrompt = "You are an Android agent. Use the available tools to interact with the phone. Only use the provided tools."

    val planningPrompt = "Given the user's instruction, create a step-by-step plan and describe what success looks like."

    val reflectionPrompt = "Reflect on the last action and the current state. Did the action make progress toward the goal? What should be done next?"

    val stoppingPrompt = "Based on the plan and progress so far, should the agent stop? Reply with 'yes' or 'no' and explain."

    val loopBreakingPrompt = "You seem to be repeating the same action or not making progress. Try a new approach or stop if stuck."
} 