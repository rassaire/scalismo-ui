package org.statismo.stk.ui.swing.actions.scenetree

import org.statismo.stk.ui.{Reloadable, SceneTreeObject}

class ReloadReloadableAction extends SceneTreePopupAction("Reload") {
  def isContextSupported(context: Option[SceneTreeObject]) = {
    context match {
      case Some(rld: Reloadable) => rld.isCurrentlyReloadable
      case _ => false
    }
  }

  override def apply(context: Option[SceneTreeObject]) = {
    if (isContextSupported(context)) {
      context.get.asInstanceOf[Reloadable].reload()
    }
  }
}