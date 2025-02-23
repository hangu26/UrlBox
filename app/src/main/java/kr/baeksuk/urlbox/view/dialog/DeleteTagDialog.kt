package kr.baeksuk.urlbox.view.dialog

import android.app.Dialog
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.DialogDeleteTagBinding

class DeleteTagDialog(private val context : AppCompatActivity) {

    private lateinit var binding : DialogDeleteTagBinding
    private val dlg = Dialog(context)
    private var deleteListener : DeleteListener? = null
    private var txTag = ""

    fun setDeleteListener(listener: DeleteListener, tag : String) {
        deleteListener = listener
        txTag = tag
    }

    fun show(){
        binding = DialogDeleteTagBinding.inflate(context.layoutInflater)

        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dlg.setContentView(binding.root)
        dlg.setCancelable(false)
        dlg.window!!.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dlg.window!!.setBackgroundDrawableResource(R.drawable.border_dialog_delete_tag)

        val dialogScope = CoroutineScope(Dispatchers.Main + Job())

        binding.btnDelete.setOnClickListener {
            dialogScope.launch {
                deleteListener?.onDeleteTag(txTag)
                dlg.dismiss()
            }
        }

        binding.btnCancel.setOnClickListener {
            dlg.dismiss()
        }

        dlg.show()

    }

    interface DeleteListener {
        suspend fun onDeleteTag(tag : String)
    }

}