package ui

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.TransferHandler

class ListItemTransferHandler : TransferHandler() {
    private val localObjectFlavor = DataFlavor(String::class.java, "Local String List Item")
    private var sourceIndex = -1

    override fun createTransferable(c: JComponent): Transferable? {
        val list = c as? JList<*> ?: return null
        this.sourceIndex = list.selectedIndex
        val value = list.selectedValue as? String ?: return null
        return object : Transferable {
            override fun getTransferDataFlavors(): Array<DataFlavor> {
                return arrayOf(localObjectFlavor)
            }

            override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
                return localObjectFlavor == flavor
            }

            override fun getTransferData(flavor: DataFlavor): Any {
                if (this.isDataFlavorSupported(flavor)) {
                    return value
                }
                throw UnsupportedFlavorException(flavor)
            }
        }
    }

    override fun canImport(support: TransferSupport): Boolean {
        return support.isDataFlavorSupported(this.localObjectFlavor) && support.dropLocation is JList.DropLocation
    }

    override fun importData(support: TransferSupport): Boolean {
        if (this.canImport(support)) {
            val dropLocation = support.dropLocation as JList.DropLocation
            val list = support.component as JList<*>
            val model = this.getModel(list) ?: return false
            val dropIndex = dropLocation.index
            val value = support.transferable.getTransferData(this.localObjectFlavor) as String
            if (this.sourceIndex == dropIndex) {
                return true
            }
            model.removeAt(this.sourceIndex)
            val insertIndex = if (dropIndex > this.sourceIndex) dropIndex - 1 else dropIndex
            model.add(insertIndex, value)
            list.selectedIndex = insertIndex
            return true
        }
        return false
    }

    @Suppress("UNCHECKED_CAST")
    private fun getModel(list: JList<*>): NonRepetitiveListModel<String>? {
        val model = list.model as? NonRepetitiveListModel<*> ?: return null
        if (model.filterIsInstance<String>().size == model.size) {
            return model as NonRepetitiveListModel<String>
        }
        return null
    }

    override fun getSourceActions(c: JComponent): Int {
        return MOVE
    }
}