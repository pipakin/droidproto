package com.hermit.droidproto;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;


public class RepRapSurface extends View {
	
	public RepRapSurface(Context context)
    {
        super(context);
    }
    public RepRapSurface(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }
    public RepRapSurface(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
	    setMeasuredDimension(height,height);
	}
}
