package  com.hwj.cook
class DesktopPlatform :Platform{
    override val name: String
        get() = "desktop> ${System.getProperty("os.name")}"

}

actual fun getPlatform():Platform = DesktopPlatform()