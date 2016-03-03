package scalismo.ui.view

import java.awt
import java.awt.event.{ MouseEvent, MouseAdapter }
import java.util.EventObject
import javax.swing.event.{ TreeSelectionEvent, TreeSelectionListener }
import javax.swing.plaf.basic.BasicTreeUI
import javax.swing.tree._
import javax.swing.{ Icon, JTree }

import scalismo.ui.model.{ Scene, SceneNode }
import scalismo.ui.model.capabilities.CollapsableView
import scalismo.ui.view.NodesPanel.{ SceneNodeCellRenderer, ViewNode }
import scalismo.utils.Benchmark

import scala.collection.JavaConversions.enumerationAsScalaIterator
import scala.collection.immutable
import scala.swing.{ BorderPanel, Component, ScrollPane }
import scala.util.Try

object NodesPanel {

  class ViewNode(backend: SceneNode) extends DefaultMutableTreeNode(backend) {
    override def getUserObject: SceneNode = {
      super.getUserObject.asInstanceOf[SceneNode]
    }
  }

  class SceneNodeCellRenderer extends DefaultTreeCellRenderer {

    class Icons(open: Icon, closed: Icon, leaf: Icon) {
      // the invocation context is a call to getTreeCellRendererComponent().
      def apply() = {
        setOpenIcon(open)
        setClosedIcon(closed)
        setLeafIcon(leaf)
      }
    }

    object Icons {
      /* note: this uses the "closed" icon for leaves. */
      final val DefaultIcons = new Icons(HighDpi.scaleIcon(getDefaultOpenIcon), HighDpi.scaleIcon(getDefaultClosedIcon), HighDpi.scaleIcon(getDefaultClosedIcon))

      def getForNode(node: SceneNode): Icons = {
        node match {
          //          case vis: VisualizableSceneTreeObject[_] =>
          //            IconFactory.iconFor(vis) match {
          //              case None => DefaultIcons
          //              case Some(icon) =>
          //                new Icons(icon, icon, icon)
          //            }
          case _ => DefaultIcons
        }
      }
    }

    private var recursingInGetRendererComponent = false

    override def getTreeCellRendererComponent(tree: JTree, value: scala.Any, sel: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean): awt.Component = {
      val sceneNode = value.asInstanceOf[ViewNode].getUserObject

      Icons.getForNode(sceneNode).apply()

      val component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)

      /* Next,we try to set the width of the component to extend (almost) to the right edge of the containing tree.
       * This has two advantages: first, it should get rid of the annoying ellipses when renaming a node (e.g. "A" -> "ABC"
       * would result in the tree showing "..." instead). Second, it allows to right-click anywhere in the row to get the popup menu.
       */

      if (component == this) {
        // the tree.getPathBounds() method used below results in another call to this method, which will create a stack overflow if we don't handle it.
        if (!recursingInGetRendererComponent) {
          recursingInGetRendererComponent = true
          val Margin = 3
          val bounds = tree.getPathBounds(tree.getPathForRow(row))
          val treeWidth = tree.getWidth
          if (bounds != null && treeWidth - Margin > bounds.x) {
            val pref = component.getPreferredSize
            pref.width = treeWidth - Margin - bounds.x
            setPreferredSize(pref)
          }
          recursingInGetRendererComponent = false
        }
      }
      component
    }

    // just for visual debugging, if needed
    //setBorder(BorderFactory.createLineBorder(Color.BLACK))
  }

}

class NodesPanel(val frame: ScalismoFrame) extends BorderPanel {
  val scene = frame.scene

  val rootNode = new ViewNode(scene)

  // indicator that a synchronization between model and view is currently
  // being performed (i.e. tree is being programmatically modified)
  private var synchronizing = false

  val mouseListener = new MouseAdapter() {
    override def mousePressed(event: MouseEvent) = handle(event)

    override def mouseReleased(event: MouseEvent) = handle(event)

    def handle(event: MouseEvent) = {
      if (event.isPopupTrigger) {
        val x = event.getX
        val y = event.getY
        pathToSceneNode(tree.getPathForLocation(x, y)).foreach { node =>
          val selected = getSelectedSceneNodes
          // the action will always affect the node that was clicked. However,
          // if the clicked node is part of a multi-selection, then it will also
          // affect all other selected elements.
          val affected = if (selected.contains(node)) selected else List(node)
          println(s"TODO: popup, affected = $affected")
        }
      }
    }
  }

  val selectionListener = new TreeSelectionListener {
    override def valueChanged(e: TreeSelectionEvent): Unit = {
      if (!synchronizing) {
        frame.selectedNodes = getSelectedSceneNodes
      }
    }
  }

  val treeModel = new DefaultTreeModel(rootNode)

  val tree: JTree = new JTree(treeModel) {
    setCellRenderer(new SceneNodeCellRenderer)
    getSelectionModel.setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION)
    addTreeSelectionListener(selectionListener)
    //    addKeyListener(listener)
    addMouseListener(mouseListener)
    setExpandsSelectedPaths(true)
    setLargeModel(true)
  }

  val scroll = new ScrollPane(Component.wrap(tree))

  def pathToSceneNode(path: TreePath): Option[SceneNode] = {
    Option(path).flatMap { path => Try { path.getLastPathComponent.asInstanceOf[ViewNode].getUserObject }.toOption }
  }

  def sceneNodeToPath(node: SceneNode): Option[TreePath] = {
    def findRecursive(currentNode: ViewNode): Option[ViewNode] = {
      if (currentNode.getUserObject eq node) Some(currentNode)
      else {
        currentNode.children().foreach { child =>
          findRecursive(child.asInstanceOf[ViewNode]) match {
            case found @ Some(_) => return found
            case _ =>
          }
        }
        None
      }
    }

    val viewNode: Option[ViewNode] = findRecursive(rootNode)
    viewNode.map { defined =>
      val pathAsArray = treeModel.getPathToRoot(defined).asInstanceOf[Array[Object]]
      new TreePath(pathAsArray)
    }
  }

  // helper function for collect(), to turn e.g. a List[Option[T]] into a (purged) List[T]
  def definedOnly[T]: PartialFunction[Option[T], T] = { case option if option.isDefined => option.get }

  // currently selected nodes
  def getSelectedSceneNodes: List[SceneNode] = {
    tree.getSelectionPaths match {
      case null => Nil
      case paths => paths.toList.map(pathToSceneNode).collect(definedOnly)
    }
  }

  def setSelectedSceneNodes(nodes: immutable.Seq[SceneNode]) = {
    val paths = nodes.map(sceneNodeToPath).collect(definedOnly)
    if (paths.nonEmpty) {
      tree.setSelectionPaths(paths.toArray)
    } else {
      tree.setSelectionRow(0)
    }
  }

  def repaintTree(): Unit = {
    // try to force the tree to invalidate cached node sizes
    tree.getUI match {
      case ui: BasicTreeUI => ui.setLeftChildIndent(ui.getLeftChildIndent)
      case _ => //don't know how to handle
    }
    tree.treeDidChange()
  }

  def synchronizeWholeTree(): Unit = {
    synchronizing = true
    // save user's selection for later
    val selecteds = getSelectedSceneNodes
    synchronizeSingleNode(scene, rootNode)
    repaintTree()
    synchronizing = false
    setSelectedSceneNodes(selecteds)
  }

  def synchronizeSingleNode(model: SceneNode, view: ViewNode): Unit = {
    // this method operates at the level of a single node, and synchronizes the view
    // of that node's children.

    // don't replace this with a val, it has to be freshly evaluated every time
    def viewChildren = view.children.map(_.asInstanceOf[ViewNode]).toList

    def nodeOrChildrenIfCollapsed(node: SceneNode): Seq[SceneNode] = {
      node match {
        case c: CollapsableView if c.isViewCollapsed => node.children.flatMap(nodeOrChildrenIfCollapsed)
        case _ => List(node)
      }
    }

    val modelChildren = model.children.flatMap(nodeOrChildrenIfCollapsed)

    // remove (obsolete) children that are in view, but not in model
    // Note: don't replace the exists with contains: we're using object identity, not "normal" equality
    viewChildren.filterNot({
      n => modelChildren.exists(_ eq n.getUserObject)
    }).foreach(treeModel.removeNodeFromParent(_))

    val existingNodesInView = viewChildren.map(_.getUserObject)

    val nodesToAddToView = modelChildren.zipWithIndex.filterNot {
      case (o, _) => existingNodesInView.exists(_ eq o)
    }

    nodesToAddToView.foreach({
      case (obj, idx) =>
        val node = new ViewNode(obj)
        treeModel.insertNodeInto(node, view, idx)
        // this ensures the tree gets expanded to show newly added nodes
        val p = node.getPath.map(_.asInstanceOf[Object])
        tree.setSelectionPath(new TreePath(p))
    })

    // recurse
    modelChildren.zip(viewChildren).foreach {
      case (m, v) => synchronizeSingleNode(m, v)
    }

  }

  //constructor logic
  layout(scroll) = BorderPanel.Position.Center

  synchronizeWholeTree()

  listenTo(scene, frame)

  reactions += {
    case ScalismoFrame.event.SelectedNodesChanged(_) => setSelectedSceneNodes(frame.selectedNodes)
    case Scene.event.SceneChanged(_) => synchronizeWholeTree()
  }

}
