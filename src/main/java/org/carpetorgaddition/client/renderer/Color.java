package org.carpetorgaddition.client.renderer;

/**
 * @param red   红色
 * @param green 绿色
 * @param blue  蓝色
 * @param alpha 不透明度，值越低模型越透明
 */
public record Color(float red, float green, float blue, float alpha) {
}