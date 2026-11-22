package app.badcat.client

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.ImageView

/** Original personal-center history and download icons, prepared for tinting. */
internal object BadcatMineIcons {
    private const val HISTORY = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAACmElEQVR42s2XwU4UQRCGv55BNJAguNw44p4ExUhE452riQ9BeANvHnwCjTF6VeKZow9g1JNRCZKQlYSDRxEwxmSX7P5e/iHNZHZ2GI1sJ5Xp6a7+/+rqmarqIIkKLQEC0KVaSwEBvUGKIwPmgyUDmgCuAteAy8Alj/8AvgKfgQ3gZ2S4LMVNUj9Jo/68pMeSdjW47Vp3vg/WCQl9jiC1u6eBB8AqcM5zH4D3QAv47rFpoAncBm547Ah4Djy0Xlp4hCU7vyVpy7vqSHomabFsN55btG7Ha7eMVeiJfuTLkg4M8EbSQk5vxJJasvdYZ8FrZazlIiPiBYmfS5IOvfCFpLFoYSIp5AyOAYN1srExY8iYSzkuYvJE0pSkbS9Yq/IRVfyI14y5bY6Mj7zyEyu+kzQeK+YkWO5bQs4z+Y2NG1PmOOaMya9Iakv6JWmuZOcZ0aSkI8tkbq7IE3PGbpsLSWkS/RCrwCjwEth0kOoOCGKHlrKA1vX8prFHzXXiN5yQ9E1S119vKDn3bJcNSfuWRokHMi8EY3fNNSGJzAPzwAzwyaG0Uhw/ResZc8McM+Y8NuC6Fd5aOS2N38XJqkjSzNHu98whcx6f3ayTTitKQqdpvyt4LMNsuT8bGzDl515kMRUAO8BF4BXQzmW/1Lt9lFu3F3OO/MW5Jk7P54G7fXRuAk+dmErrgX0/GxWOIPPOAXAPuFBAIGfPL/ZSjNeIOTMDdryoeYoj6ACvK3orLkqa7u/EBny0lXfs2q7fVaH0CgN+v15UziXmCOasFYiomZj6BqLUNdy6LVzxzsM/DESZN1fMsW7OtE4yqpuWC5NR3XRcVSql46EoSOqWZEWZslZJNhRF6ZmU5UN5MRmKq9l/u5yGs76e/wHX5ODXprGy2AAAAABJRU5ErkJggg=="
    private const val DOWNLOAD = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAACC0lEQVR42uWXPWsVQRSGn5ldYxojFoKpNI0gKa0s/AWW4tcfiB8QglrYScQoKAhqaecPErGys4pa2CgRzMfNfW3O4mGZvbuze8kWDgw7e+7MnGfeM/fMbJDEmCX2HBfsec+qt+VN1EOBAAhYAn7YIk4DP91vnUs5QL0S2AEKYOGoQwAwtRXHMfZAaj8cOYBcvMNYAJXz8N8qoDEVwAH03pBxwO5WRt+QC1BltDiHEMSEUq0Asgw3benT9i+INkfRlKJjw/tl4BOw7iYILWFIpeopcBP4CFw1ezHrLKicnABWgbc24LX1nXRUoOp7C3gPHANOpnWU6jXa847+lftmKyUFax+X9E3SnqSzbmxp7RuS9m38Y7OFur8UgIe46yAeOAgkLUj6agDnnA1J1yXt2rgnbs7OAN5RCqJwAPuSVhz0Nbfyp65/SPmZBeAhfDgemm3RAZx3K9+zflttzrsANEFUSmxLmko6JemKk/3ZLNlzATzEbQfxSNIXSTsG9Nvsz7usPBfAQ6w5iIlJPrH3FznOcwFSSkwkHVj7Za7zPgBNSrzqGvN67XMrPrRM985S9xlg0x06Wdfy1HdB0XLTkbsNH9Tyu1rOjGALGPRhMtdS1g6iAGwAF2ed4QNuTh+ANz5UlQKVo0Xge+PJNbz8ApaB3QqsrBH+AS4BF9yXz7wUiMBn8xHqCoxW/gLBnuuKLlN0iQAAAABJRU5ErkJggg=="

    fun create(context: Context, key: String, tint: Int): ImageView {
        val encoded = if (key == "history") HISTORY else DOWNLOAD
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        return ImageView(context).apply {
            setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            setColorFilter(tint)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = if (key == "history") "阅读时间" else "下载管理"
        }
    }
}
