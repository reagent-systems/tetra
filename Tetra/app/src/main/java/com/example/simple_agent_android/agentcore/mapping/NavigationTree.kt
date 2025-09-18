package com.example.simple_agent_android.agentcore.mapping

import java.util.concurrent.ConcurrentHashMap

/**
 * Represents a hierarchical tree structure of the phone's UI navigation
 * Root is always the home screen, with branches representing different apps and screens
 */
class NavigationTree {
    private val nodes = ConcurrentHashMap<String, TreeNode>()
    private val rootNodeId = "home_screen"
    
    init {
        // Initialize with root node
        nodes[rootNodeId] = TreeNode(
            id = rootNodeId,
            screenState = null,
            parent = null,
            children = mutableSetOf(),
            depth = 0
        )
    }
    
    /**
     * Add a new screen state to the tree
     */
    fun addScreenState(screenState: ScreenState, parentId: String? = null): String {
        val parent = if (parentId != null) nodes[parentId] else nodes[rootNodeId]
        if (parent == null) {
            throw IllegalArgumentException("Parent node not found: $parentId")
        }
        
        val nodeId = generateNodeId(screenState)
        val node = TreeNode(
            id = nodeId,
            screenState = screenState,
            parent = parent,
            children = mutableSetOf(),
            depth = parent.depth + 1
        )
        
        nodes[nodeId] = node
        parent.children.add(node)
        
        return nodeId
    }
    
    /**
     * Find a screen state by ID
     */
    fun findScreenState(screenId: String): ScreenState? {
        return nodes[screenId]?.screenState
    }
    
    /**
     * Find the most similar screen state
     */
    fun findSimilarScreenState(screenState: ScreenState, threshold: Float = 0.8f): TreeNode? {
        return nodes.values
            .filter { it.screenState != null }
            .maxByOrNull { it.screenState!!.calculateSimilarity(screenState) }
            ?.takeIf { it.screenState!!.calculateSimilarity(screenState) >= threshold }
    }
    
    /**
     * Get the path from one screen to another
     */
    fun getPath(fromScreenId: String, toScreenId: String): List<NavigationPath>? {
        val fromNode = nodes[fromScreenId] ?: return null
        val toNode = nodes[toScreenId] ?: return null
        
        // Find common ancestor
        val commonAncestor = findCommonAncestor(fromNode, toNode)
        if (commonAncestor == null) return null
        
        // Build path from fromNode to common ancestor (upward)
        val upwardPath = mutableListOf<TreeNode>()
        var current = fromNode
        while (current != commonAncestor) {
            upwardPath.add(current)
            current = current.parent ?: break
        }
        
        // Build path from common ancestor to toNode (downward)
        val downwardPath = mutableListOf<TreeNode>()
        current = toNode
        while (current != commonAncestor) {
            downwardPath.add(current)
            current = current.parent ?: break
        }
        downwardPath.reverse()
        
        // Combine paths
        val fullPath = upwardPath + downwardPath
        
        // Convert to NavigationPath objects
        return buildNavigationPaths(fullPath)
    }
    
    /**
     * Get all children of a screen
     */
    fun getChildren(screenId: String): List<ScreenState> {
        return nodes[screenId]?.children?.mapNotNull { it.screenState } ?: emptyList()
    }
    
    /**
     * Get all screens in a specific app
     */
    fun getScreensInApp(packageName: String): List<ScreenState> {
        return nodes.values
            .mapNotNull { it.screenState }
            .filter { it.appPackage == packageName }
    }
    
    /**
     * Mark a screen as obsolete (UI has changed)
     */
    fun markScreenObsolete(screenId: String) {
        nodes[screenId]?.let { node ->
            node.screenState?.let { screenState ->
                val updatedScreenState = screenState.copy(
                    confidence = 0.0f,
                    metadata = screenState.metadata + ("obsolete" to true)
                )
                node.screenState = updatedScreenState
            }
        }
    }
    
    /**
     * Clean up obsolete screens
     */
    fun cleanupObsoleteScreens() {
        val obsoleteNodes = nodes.values.filter { 
            it.screenState?.metadata?.get("obsolete") == true 
        }
        
        obsoleteNodes.forEach { node ->
            // Remove from parent's children
            node.parent?.children?.remove(node)
            
            // Remove all children recursively
            removeNodeRecursively(node)
            
            // Remove from nodes map
            nodes.remove(node.id)
        }
    }
    
    /**
     * Get the tree structure as a string for debugging
     */
    fun getTreeString(): String {
        return buildTreeString(nodes[rootNodeId]!!, 0)
    }
    
    private fun generateNodeId(screenState: ScreenState): String {
        val baseId = "${screenState.appPackage}_${screenState.screenTitle ?: "unknown"}"
        var counter = 1
        var nodeId = baseId
        
        while (nodes.containsKey(nodeId)) {
            nodeId = "${baseId}_$counter"
            counter++
        }
        
        return nodeId
    }
    
    private fun findCommonAncestor(node1: TreeNode, node2: TreeNode): TreeNode? {
        val path1 = mutableListOf<TreeNode>()
        var current = node1
        while (current.parent != null) {
            path1.add(current)
            current = current.parent!!
        }
        path1.add(current) // Add root
        
        val path2 = mutableListOf<TreeNode>()
        current = node2
        while (current.parent != null) {
            path2.add(current)
            current = current.parent!!
        }
        path2.add(current) // Add root
        
        // Find first common node
        for (n1 in path1) {
            for (n2 in path2) {
                if (n1.id == n2.id) {
                    return n1
                }
            }
        }
        
        return null
    }
    
    private fun buildNavigationPaths(nodes: List<TreeNode>): List<NavigationPath> {
        val paths = mutableListOf<NavigationPath>()
        
        for (i in 0 until nodes.size - 1) {
            val fromNode = nodes[i]
            val toNode = nodes[i + 1]
            
            if (fromNode.screenState != null && toNode.screenState != null) {
                // Find navigation path between these screens
                val path = fromNode.screenState!!.navigationPaths.find { 
                    it.toScreenId == toNode.id 
                }
                
                if (path != null) {
                    paths.add(path)
                }
            }
        }
        
        return paths
    }
    
    private fun removeNodeRecursively(node: TreeNode) {
        node.children.forEach { child ->
            removeNodeRecursively(child)
            nodes.remove(child.id)
        }
        node.children.clear()
    }
    
    private fun buildTreeString(node: TreeNode, depth: Int): String {
        val indent = "  ".repeat(depth)
        val screenInfo = node.screenState?.let { 
            " (${it.appPackage} - ${it.screenTitle ?: "Unknown"})" 
        } ?: ""
        
        val result = StringBuilder("$indent- ${node.id}$screenInfo\n")
        
        node.children.forEach { child ->
            result.append(buildTreeString(child, depth + 1))
        }
        
        return result.toString()
    }
    
    /**
     * Get statistics about the tree
     */
    fun getStatistics(): TreeStatistics {
        val totalNodes = nodes.size
        val screenNodes = nodes.values.count { it.screenState != null }
        val obsoleteNodes = nodes.values.count { 
            it.screenState?.metadata?.get("obsolete") == true 
        }
        val maxDepth = nodes.values.maxOfOrNull { it.depth } ?: 0
        
        return TreeStatistics(
            totalNodes = totalNodes,
            screenNodes = screenNodes,
            obsoleteNodes = obsoleteNodes,
            maxDepth = maxDepth
        )
    }
}

data class TreeNode(
    val id: String,
    var screenState: ScreenState?,
    val parent: TreeNode?,
    val children: MutableSet<TreeNode>,
    val depth: Int
)

data class TreeStatistics(
    val totalNodes: Int,
    val screenNodes: Int,
    val obsoleteNodes: Int,
    val maxDepth: Int
)
