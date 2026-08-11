package boat.carpetorgaddition.wheel;

@SuppressWarnings("unused")
public class ColorValue {
    private int rgba;

    private ColorValue(int rgba) {
        this.rgba = rgba;
    }

    public static ColorValue of() {
        return new ColorValue(0);
    }

    public static ColorValue fromRgba(int rgb, int alpha) {
        int rgba = ((rgb & 0x00FFFFFF) << 8) | (alpha & 0xFF);
        return new ColorValue(rgba);
    }

    public int getRed() {
        return this.rgba >> 24 & 0xFF;
    }

    public int getGreen() {
        return this.rgba >> 16 & 0xFF;
    }

    public int getBlue() {
        return this.rgba >> 8 & 0xFF;
    }

    public int getAlpha() {
        return this.rgba & 0xFF;
    }

    public void setRed(int red) {
        this.rgba = (this.rgba & 0x00FFFFFF) | ((red & 0xFF) << 24);
    }

    public void setGreen(int green) {
        this.rgba = (this.rgba & 0xFF00FFFF) | ((green & 0xFF) << 16);
    }

    public void setBlue(int blue) {
        this.rgba = (this.rgba & 0xFFFF00FF) | ((blue & 0xFF) << 8);
    }

    public void setAlpha(int alpha) {
        this.rgba = (this.rgba & 0xFFFFFF00) | (alpha & 0xFF);
    }

    public int toRgba() {
        return this.rgba;
    }

    public int toArgb() {
        return Integer.rotateRight(this.rgba, 8);
    }

    public void setRgba(int rgba) {
        this.rgba = rgba;
    }
}
