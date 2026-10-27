package de.christina.atmen;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/** Ein weicher Kreis, der mit dem Atem wächst und schrumpft. */
public class BreathView extends View {

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guide = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final float GLOW = 1.28f;
    private float level = 0f;

    public BreathView(Context context) {
        super(context);
        // Verläufe für einen Kreis mit Radius 1, beim Zeichnen skaliert.
        fill.setShader(new RadialGradient(0, 0, 1f,
            new int[] { 0xFF5F6BC2, 0xFF3A4389 }, null, Shader.TileMode.CLAMP));
        glow.setShader(new RadialGradient(0, 0, GLOW,
            new int[] { 0x557B8CDE, 0x557B8CDE, 0x007B8CDE },
            new float[] { 0f, 1f / GLOW, 1f }, Shader.TileMode.CLAMP));
        guide.setStyle(Paint.Style.STROKE);
        guide.setColor(0x33A9B2E8);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public void setLevel(float level) {
        if (level != this.level) {
            this.level = level;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float maxR = Math.min(getWidth(), getHeight()) / 2f * 0.78f;
        float minR = maxR * 0.42f;
        float r = minR + (maxR - minR) * level;

        // Dünner Ring zeigt, wie groß der Kreis beim Einatmen wird.
        guide.setStrokeWidth(getResources().getDisplayMetrics().density * 1.5f);
        canvas.drawCircle(cx, cy, maxR, guide);

        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(r, r);
        canvas.drawCircle(0, 0, GLOW, glow);
        canvas.drawCircle(0, 0, 1f, fill);
        canvas.restore();
    }
}
