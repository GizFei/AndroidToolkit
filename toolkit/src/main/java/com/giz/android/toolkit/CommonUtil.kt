/**
 * @author GizFei
 * Android常用工具函数集
 * Android common tool functions set
 */

package com.giz.android.toolkit

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.WindowManager
import androidx.annotation.ColorInt
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection

/**
 * 判断机子的Android版本是否大于或等于（>=）minVersion
 * @param minVersion 最低版本（Build.VERSION_CODES.*）
 * @see Build.VERSION_CODES
 */
fun judgeVersion(minVersion: Int) : Boolean = Build.VERSION.SDK_INT >= minVersion

/**
 * dp转px
 * @param dp dp值
 * @param context 上下文
 * @return px像素值（Float）
 * @see dp2pxSize(Context, Float)
 */
fun dp2px(context: Context, dp: Float) : Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
    dp, context.resources.displayMetrics)
/**
 * dp转px整数值
 * @param dp dp值
 * @param context 上下文
 * @return px像素值（Int）
 * @see dp2px(Context, Float)
 */
fun dp2pxSize(context: Context, dp: Float) : Int = dp2px(context, dp).toInt()

/**
 * 获取屏幕度量参数
 */
private fun getWindowMetrics(context: Context) : DisplayMetrics? {
    val wm : WindowManager? = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    if(wm != null){
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getMetrics(metrics)
        return metrics
    }
    return null
}
/**
 * 获得屏幕的宽
 * 默认宽度：1080
 * @param context 上下文
 * @return 宽度：Int
 * @see getScreenHeight
 */
fun getScreenWidth(context: Context) : Int = getWindowMetrics(context)?.widthPixels ?: 1080
/**
 * 获得屏幕的高
 * 默认高度：1920
 * @param context 上下文
 * @return 高度：Int
 * @see getScreenWidth
 */
fun getScreenHeight(context: Context) : Int = getWindowMetrics(context)?.heightPixels ?: 1920

/**
 * 因为String的length是字符数量不是字节数量所以为了防止中文字符过多，把4*1024的MAX字节打印长度改为2001字符数
 * @param tag 标签
 * @param msg 信息
 */
fun logAllContent(tag: String, msg: String){
    var tmpMsg = msg
    val maxStrLength = 2001 - tag.length
    while (tmpMsg.length > maxStrLength){
        Log.d(tag, tmpMsg.substring(0, maxStrLength))
        tmpMsg = tmpMsg.substring(maxStrLength)
    }
    Log.d(tag, tmpMsg) // 剩余部分
}

/**
 * 抓取网页源码
 * @param connection 连接
 * @return 源码字符串
 */
fun fetchHtml(connection: HttpURLConnection) : String {
    val inputStream: InputStream = connection.inputStream
    val result = ByteArrayOutputStream()
    val buffer = ByteArray(1024)
    var length: Int = inputStream.read(buffer)
    while(length != -1){
        result.write(buffer, 0, length)
        length = inputStream.read(buffer)
    }
    result.close()
    return result.toString()
}

/**
 * 判断一个字符串是否为网址
 * @param url 字符串
 * @param loose 判断条件是否严格，如果为true，则网址开头可以不带“https/http”
 */
fun isUrl(url: String?, loose: Boolean = false) : Boolean {
    val expr = "(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]"
    val looseExpr = "[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]"
    return when (loose) {
        true -> (url?.matches(Regex(expr)) ?: false) || (url?.matches(Regex(looseExpr)) ?: false)
        false -> url?.matches(Regex(looseExpr)) ?: false
    }
}

/**
 * 获得用于设置**tint属性的值
 * @param color #AARRGGBB
 */
fun getColorStateList(@ColorInt color: Int) = ColorStateList(getEmptyColorStateList(), intArrayOf(color))
/**
 * 返回空的ColorStateList
 * @return Array<IntArray>
 */
fun getEmptyColorStateList() : Array<IntArray> = arrayOf(intArrayOf(0))

/* ===================== */
/* Drawable与Bitmap互转   */
/* ===================== */
/**
 * drawable转bitmap
 * @param drawable
 * @return bitmap
 * @see bitmap2Drawable
 */
fun drawable2Bitmap(drawable: Drawable?) : Bitmap? = (drawable as BitmapDrawable).bitmap
/**
 * bitmap转drawable
 * @param context 上下文
 * @param bitmap 不能为空
 * @return drawable
 * @see drawable2Bitmap
 */
fun bitmap2Drawable(context: Context, bitmap: Bitmap) : Drawable? = BitmapDrawable(context.resources, bitmap)

/**
 * 高斯模糊
 * @param radius 模糊半径：0 < radius <= 25
 * @param origin 原图
 * @param recycle 是否回收原图，默认false
 */
fun gaussianBlur(context: Context, radius: Float, origin: Bitmap, recycle: Boolean = false) : Bitmap {
    val renderScript = RenderScript.create(context)
    val outBmp = Bitmap.createBitmap(origin.width, origin.height, origin.config)
    val input = Allocation.createFromBitmap(renderScript, origin)
    val output = Allocation.createTyped(renderScript, input.type)
    val scriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
    scriptIntrinsicBlur.setRadius(radius)
    scriptIntrinsicBlur.setInput(input)
    scriptIntrinsicBlur.forEach(output)
    output.copyTo(outBmp)
    if(recycle){
        origin.recycle()
    }
    return outBmp
}

/**
 * 改变bitmap大小
 * @param width 新图片宽度
 * @param height 新图片高度
 * @param recycle 是否回收原图
 * @return bitmap
 * @see resizeBitmapByRatio
 */
fun resizeBitmap(origin: Bitmap, width: Int, height: Int, recycle: Boolean) : Bitmap? {
    if(width <= 0 && height <= 0){
        return null
    }
    val matrix = Matrix()
    matrix.postScale(width.toFloat() / origin.width, height.toFloat() / origin.height)
    val resizeBmp = Bitmap.createBitmap(origin, 0, 0, origin.width, origin.height, matrix, false)
    if(recycle){
        origin.recycle()
    }
    return resizeBmp
}

/**
 * 按比例整体缩放bitmap
 * @param origin 原图
 * @param radio 比例，> 0
 * @param recycle 是否回收原图
 * @return bitmap
 * @see resizeBitmap
 */
fun resizeBitmapByRatio(origin: Bitmap, radio: Float, recycle: Boolean) : Bitmap? {
    if(radio <= 0){
        return null
    }
    val matrix = Matrix()
    matrix.postScale(origin.width * radio, origin.height * radio)
    val resizeBmp = Bitmap.createBitmap(origin, 0, 0, origin.width, origin.height, matrix, false)
    if(recycle){
        origin.recycle()
    }
    return resizeBmp
}