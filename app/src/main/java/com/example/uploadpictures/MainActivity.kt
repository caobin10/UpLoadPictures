package com.example.uploadpictures

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.jakewharton.rxbinding2.view.RxView
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_CODE_TAKE_PHOTO = 101
        const val REQUEST_CODE_PICK_PHOTO = 102
    }

    private lateinit var pictures: MutableList<Picture>
    private lateinit var pictureAdapter: BaseQuickAdapter<Picture, BaseViewHolder>

    //图片
    private var currentPictureUri: Uri? = null
    private var currentPictureFile: File? = null
//    private val pictureAddressSet = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        initListener()
        pictures = mutableListOf(Picture())
        pictureAdapter =
            object : BaseQuickAdapter<Picture, BaseViewHolder>(R.layout.item_suggestion_picture) {
                override fun convert(helper: BaseViewHolder, item: Picture?) {
                    helper.apply {
                        val pictureIv = getView<ImageView>(R.id.iv_picture)
                        val deleteIv = getView<ImageView>(R.id.iv_delete)
                        val addPictureTv = getView<TextView>(R.id.tv_add_picture)
                        val contentCl = getView<ConstraintLayout>(R.id.cl_content)
//                        contentCl.layoutParams = ConstraintLayout.LayoutParams(
//                            windowManager.defaultDisplay.width / 4,
//                            windowManager.defaultDisplay.width / 4
//                        )
                        contentCl.layoutParams = ConstraintLayout.LayoutParams(
                            windowManager.defaultDisplay.width / 3,
                            windowManager.defaultDisplay.width / 3
                        )

                        if (item?.uri == null) {
                            //添加图片
                            deleteIv.visibility = View.GONE
                            pictureIv.visibility = View.GONE
                            addPictureTv.visibility = View.VISIBLE

                            RxView.clicks(addPictureTv)
                                .throttleFirst(2, TimeUnit.SECONDS)
                                .compose(
                                    RxPermissions(this@MainActivity)
                                        .ensure(
                                            Manifest.permission.READ_EXTERNAL_STORAGE,
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                            Manifest.permission.CAMERA
                                        )
                                ).subscribe {
                                    if (it) {
                                        if (!isDestroyed) {
                                            MaterialDialog(mContext).title(text = "请选择")
                                                .cancelable(true).show {
                                                    listItems(
                                                        items = mutableListOf(
                                                            "拍照",
                                                            "相册"
                                                        )
                                                    ) { dialog, index, text ->
                                                        if (text == "拍照") {
                                                            takePhoto()
                                                        } else {
                                                            takeGallery()
                                                        }
                                                    }
                                                }
                                        }
                                    } else {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "缺少拍照或存储权限,请到设置中开启后使用",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        } else {
                            //显示图片
                            deleteIv.visibility = View.VISIBLE
                            pictureIv.visibility = View.VISIBLE
                            addPictureTv.visibility = View.GONE
                            Glide.with(this@MainActivity).load(item.uri).into(pictureIv)
                            deleteIv.setOnClickListener {
                                pictures.removeAt(adapterPosition)
                                tv_picture_num.text = "(${pictures.size - 1}/9)"
                                notifyDataSetChanged()
                            }
                        }
                    }
                }

                override fun getItemCount(): Int {
                    return if (pictures.size > 5) {
//                        5
                        9
                    } else {
                        pictures.size
                    }
                }
            }
        recyclerview.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 3, RecyclerView.VERTICAL, false)
            adapter = pictureAdapter
        }
        pictureAdapter.setNewData(pictures)
    }

//    private fun initListener() {
//        tv_submit.setOnClickListener {
//            submit()
//        }
//    }

//    private fun submit() {
//        //加载框
//        pictureAddressSet.clear()
//        //1.循环将图片上传服务器
//        if (pictures.size > 1) {
//            pictures.filter {
//                it.file != null
//            }.forEach {
//                val outFilePath =
//                    getExternalFilesDir(null)?.absolutePath + File.separator + System.currentTimeMillis() + "temp.jpg"
//                val requestBody = it.uri!!
//                    .copyAndConvert(this, outFilePath)
//                    .asRequestBody("multipart/form-data".toMediaTypeOrNull())
//                val part = MultipartBody.Part.createFormData("file", it.file?.name, requestBody)
//                //网络请求，把图片上传
////                 mViewModel.uploadFile(part)
//            }
//        }
//    }


//    override fun startObserve() {
//        super.startObserve()
//        //2.图片提交成功
//        uploadFileSuccess.observe(this@MainActivity, androidx.lifecycle.Observer {
//            pictureAddressSet.add(it.fileUrl)
//            if (pictureAddressSet.size == pictures.size - 1) {
//                orderOutsideHotelRequest?.fileKeys = pictureAddressSet.toMutableList()
//                //3.图片上传完以后再走提交操作
//                mViewModel.createReimbursement(orderOutsideHotelRequest!!)
//            }
//        })
//    }


    /**
     * 相册
     */
    private fun takeGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE_PICK_PHOTO)
    }

    /**
     * 拍照
     */
    private fun takePhoto() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val fileName = "${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.CHINA).format(Date())}.png"
            val photoFile = File(getExternalFilesDir(null), fileName)
            currentPictureFile = photoFile
            currentPictureUri = FileProviderForAPI24.getUriForFile(this, photoFile)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPictureUri)
            startActivityForResult(takePictureIntent, REQUEST_CODE_TAKE_PHOTO)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_TAKE_PHOTO -> {
                    val picture = Picture()
                    picture.file = currentPictureFile
                    picture.uri = currentPictureUri
                    pictures.add(pictures.size - 1, picture)
                    pictureAdapter.notifyDataSetChanged()
                    tv_picture_num.text = "(${pictures.size - 1}/9)"
                }
                REQUEST_CODE_PICK_PHOTO -> {
                    currentPictureUri = data?.data
                    currentPictureUri?.apply {
                        val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
                        val cursor = contentResolver.query(
                            currentPictureUri!!,
                            filePathColumn,
                            null,
                            null,
                            null
                        )
                        cursor?.moveToFirst()
                        val path =
                            cursor?.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
                        cursor?.close()
                        currentPictureFile = File(path)
                        val picture = Picture()
                        picture.file = currentPictureFile
                        picture.uri = currentPictureUri
                        pictures.add(pictures.size - 1, picture)
                        pictureAdapter.notifyDataSetChanged()
                        tv_picture_num.text = "(${pictures.size - 1}/9)"
                    }
                }
            }
        }
    }
}

class Picture {
    var uri: Uri? = null
    var file: File? = null
}