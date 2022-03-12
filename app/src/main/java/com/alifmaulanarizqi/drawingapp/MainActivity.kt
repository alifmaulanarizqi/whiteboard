package com.alifmaulanarizqi.drawingapp

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yuku.ambilwarna.AmbilWarnaDialog
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.Exception


class MainActivity : AppCompatActivity() {
    private lateinit var drawingView: DrawingView
    // private lateinit var activeColor: ImageButton
    private lateinit var mColorPreview: View
    private lateinit var ibEraser: ImageButton
    private lateinit var flCanvasContainer: FrameLayout
    private var ivBackground: ImageView? = null
    private var mDefaultColor = 0
    private var mBackgroundDefaultColor = Color.WHITE
    private var customProgressDialog: Dialog? = null

    private val requestPermissions: ActivityResultLauncher<Array<String>> = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) {
            permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if(isGranted)
                    if(permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                        intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        openGalleryLayncher.launch(intent)
                    }
                else {
                    if(permissionName == Manifest.permission.READ_EXTERNAL_STORAGE)
                        Toast.makeText(this, "Permission denied for access file", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private val openGalleryLayncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
            result ->
            if(result.resultCode == RESULT_OK && result.data != null) {
                ivBackground = findViewById(R.id.ivBackground)
                ivBackground?.setImageURI(result.data?.data)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawingView)
        drawingView.setBrushThickness(10f)

//        val llColorsContainer: LinearLayout = findViewById(R.id.llColorsContainer)
//        activeColor = llColorsContainer[1] as ImageButton
//        activeColor.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallete_selected))

        mColorPreview = findViewById(R.id.previewSelectedColor)
        mColorPreview.setOnClickListener { // to make code look cleaner the color
            // picker dialog functionality are
            // handled in openColorPickerDialogue()
            // function
            openColorPickerDialogue()
        }

        ibEraser = findViewById(R.id.ibEraser)
        ibEraser.setOnClickListener {
            val hexColor = String.format("#%06X", 0xFFFFFF and mBackgroundDefaultColor)
            drawingView.setColor(hexColor)
            ibEraser.background = ContextCompat.getDrawable(this, R.drawable.pallete_selected)
        }

        val brushDialogBtn: ImageButton = findViewById(R.id.ibBrush)
        brushDialogBtn.setOnClickListener {
            showBrushSizeDialog()
        }

        val ibUndo: ImageButton = findViewById(R.id.ibUndo)
        ibUndo.setOnClickListener {
            drawingView.undo()
        }

        val ibClear: ImageButton = findViewById(R.id.ibClear)
        ibClear.setOnClickListener {
            drawingView.clear()
            mBackgroundDefaultColor = Color.WHITE
            flCanvasContainer.setBackgroundColor(mBackgroundDefaultColor)
            if(ivBackground?.drawable != null)
                ivBackground?.setImageResource(0)
        }

        val ibRedo: ImageButton = findViewById(R.id.ibRedo)
        ibRedo.setOnClickListener {
            drawingView.redo()
        }

        flCanvasContainer = findViewById(R.id.flCanvasContainer)

//        val ibImportImage: ImageButton = findViewById(R.id.ibImportImage)
//        ibImportImage.setOnClickListener {
//            requestStoragePermission()
//        }

//        val ibSave: ImageButton = findViewById(R.id.ibSave)
//        ibSave.setOnClickListener {
//            if(isReadStorageAllowed()) {
//                showProgressDialog()
//                lifecycleScope.launch {
//                    saveBitmapFile(getBitmapFromView(flCanvasContainer))
//                }
//            }
//        }

//        val ibShare: ImageButton = findViewById(R.id.ibShare)
//        ibShare.setOnClickListener {
//            if(isReadStorageAllowed()) {
//                showProgressDialog()
//                lifecycleScope.launch {
//                    shareImage(saveBitmapFile(getBitmapFromView(flCanvasContainer)))
//                }
//            }
//        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.getItemId()

        when (id) {
            R.id.importImage -> {
                requestStoragePermission()
            }

            R.id.saveImage -> {
                if(isReadStorageAllowed()) {
                    showProgressDialog()
                    lifecycleScope.launch {
                        saveBitmapFile(getBitmapFromView(flCanvasContainer))
                    }
                }
            }

            R.id.shareImage -> {
                if(isReadStorageAllowed()) {
                    showProgressDialog()
                    lifecycleScope.launch {
                        shareImage(saveBitmapFile(getBitmapFromView(flCanvasContainer)))
                    }
                }
            }

            R.id.changeBackgroundColor -> {
                openColorPickerDialogueBackground()
            }

        }

        return super.onOptionsItemSelected(item)
    }

    private fun showBrushSizeDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)

        val smallBrush: ImageButton = brushDialog.findViewById(R.id.ibSmallBrush)
        smallBrush.setOnClickListener {
            drawingView.setBrushThickness(10f)
            brushDialog.dismiss()
        }

        val mediumBrush: ImageButton = brushDialog.findViewById(R.id.ibMediumBrush)
        mediumBrush.setOnClickListener {
            drawingView.setBrushThickness(20f)
            brushDialog.dismiss()
        }

        val largeBrush: ImageButton = brushDialog.findViewById(R.id.ibLargeBrush)
        largeBrush.setOnClickListener {
            drawingView.setBrushThickness(30f)
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

//    fun pickColor(view: View) {
//        if(view !== activeColor) {
//            val imageButton = view as ImageButton
//            drawingView.setColor(imageButton.tag.toString())
//
//            imageButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallete_selected))
//            activeColor.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallete_normal))
//
//            activeColor = view
//        }
//    }

    private fun openColorPickerDialogue() {

        // the AmbilWarnaDialog callback needs 3 parameters
        // one is the context, second is default color,
        val colorPickerDialogue = AmbilWarnaDialog(this, mDefaultColor,
            object : OnAmbilWarnaListener {
                override fun onCancel(dialog: AmbilWarnaDialog) {
                    // leave this function body as
                    // blank, as the dialog
                    // automatically closes when
                    // clicked on cancel button
                }

                override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
                    // change the mDefaultColor to
                    // change the GFG text color as
                    // it is returned when the OK
                    // button is clicked from the
                    // color picker dialog
                    mDefaultColor = color

                    // now change the picked color
                    // preview box to mDefaultColor
                    mColorPreview.setBackgroundColor(mDefaultColor)
                    val hexColor = String.format("#%06X", 0xFFFFFF and mDefaultColor)
                    drawingView.setColor(hexColor)
                    ibEraser.background = ContextCompat.getDrawable(this@MainActivity, R.drawable.pallete_normal)
                }
            })
        colorPickerDialogue.show()
    }

    private fun openColorPickerDialogueBackground() {

        // the AmbilWarnaDialog callback needs 3 parameters
        // one is the context, second is default color,
        val colorPickerDialogue = AmbilWarnaDialog(this, mBackgroundDefaultColor,
            object : OnAmbilWarnaListener {
                override fun onCancel(dialog: AmbilWarnaDialog) {
                    // leave this function body as
                    // blank, as the dialog
                    // automatically closes when
                    // clicked on cancel button
                }

                override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
                    // change the mDefaultColor to
                    // change the GFG text color as
                    // it is returned when the OK
                    // button is clicked from the
                    // color picker dialog
                    mBackgroundDefaultColor = color

                    // now change the picked color
                    // preview box to mDefaultColor
                    flCanvasContainer.setBackgroundColor(mBackgroundDefaultColor)
                    drawingView.clearEraserPath(mBackgroundDefaultColor)
                    drawingView.mPreviousBgColor = mBackgroundDefaultColor

                    val hexColor = String.format("#%06X", 0xFFFFFF and mBackgroundDefaultColor)
                    drawingView.setColor(hexColor)
                }
            })
        colorPickerDialogue.show()
    }

    private fun isReadStorageAllowed(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestStoragePermission() {
        if(shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE))
            showRationaleDialog("Permission Denied", "To import image, you must approve \"Files and media\" permission manually in settings")
        else
            requestPermissions.launch (arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
    }

    private fun showRationaleDialog(title: String, message: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgCanvas = view.background
        if(bgCanvas != null)
            bgCanvas.draw(canvas)
        else
            canvas.drawColor(Color.WHITE)

        view.draw(canvas)
        return bitmap
    }

    private suspend fun saveBitmapFile(bitmap: Bitmap?): String {
        var result = ""
        var uriString = ""

        withContext(Dispatchers.IO) {
            if(bitmap != null) {
                try {
                    val filename = "${System.currentTimeMillis()/1000}.png"
                    val mimeType =  "image/png"
                    val directory = Environment.DIRECTORY_PICTURES
                    val mediaContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

                    val imageOutStream: OutputStream

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val values = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                            put(MediaStore.Images.Media.RELATIVE_PATH, directory)
                        }

                        contentResolver.run {
                            val uri = contentResolver.insert(mediaContentUri, values)!!
                            imageOutStream = openOutputStream(uri)!!
                            result = uri.toString()
                            uriString = RealPathUtil.getRealPath(this@MainActivity, Uri.parse(result))
                        }
                    }
                    else {
                        val imagePath = Environment.getExternalStoragePublicDirectory(directory).absolutePath
                        val image = File(imagePath, filename)
                        imageOutStream = FileOutputStream(image)
                        uriString = image.absolutePath
                    }

                    imageOutStream.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }

                    runOnUiThread {
                        cancelProgressDialog()
                        if(result.isNotEmpty())
                            Toast.makeText(this@MainActivity, "File saved succesfully: $uriString", Toast.LENGTH_LONG).show()
                        else
                            Toast.makeText(this@MainActivity, "Something went wrong while saving", Toast.LENGTH_LONG).show()
                    }

                }
                catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }

        return result
    }

    private fun showProgressDialog() {
        customProgressDialog = Dialog(this)
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog?.show()
    }

    private fun cancelProgressDialog() {
        if (customProgressDialog != null) {
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareImage(result:String){
        val uri = RealPathUtil.getRealPath(this, Uri.parse(result))
        MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result), null) {_, _ ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.type = "image/png"
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(uri))
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }

    }

}