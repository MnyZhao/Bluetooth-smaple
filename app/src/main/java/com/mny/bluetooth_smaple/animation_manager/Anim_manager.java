package com.mny.bluetooth_smaple.animation_manager;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.mny.bluetooth_smaple.R;

/**
 * Crate by E470PD on 2018/9/3
 */
public class Anim_manager {
    public Anim_manager() {
    }

    public Anim_manager(MenuItem menuItem) {
        this.menuItem = menuItem;
    }

    MenuItem menuItem;
    /**
     * item做动画
     *
     * @param item
     */
    public void showAnimate(MenuItem item, Activity activity) {
        menuItem = item;
        hideAnimate();
        //这里使用一个ImageView设置成MenuItem的ActionView，这样我们就可以使用这个ImageView显示旋转动画了
        ImageView qrView = (ImageView)activity.getLayoutInflater().inflate(R.layout.action_view, null);
        qrView.setImageResource(R.drawable.ic_autorenew_black_40);
        menuItem.setActionView(qrView);
        //显示动画
        Animation animation = AnimationUtils.loadAnimation(activity,R.anim.anim_rotate);
        animation.setRepeatMode(Animation.RESTART);
        animation.setRepeatCount(Animation.INFINITE);
        qrView.startAnimation(animation);
    }
    /**
     * 关闭动画
     */
    public void hideAnimate() {
        if(menuItem != null){
            View view = menuItem.getActionView();
            if(view != null){
                view.clearAnimation();
                menuItem.setActionView(null);
            }
        }
    }
}
