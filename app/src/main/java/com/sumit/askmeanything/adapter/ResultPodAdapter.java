package com.sumit.askmeanything.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.common.util.UriUtil;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.ProgressBarDrawable;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.sumit.askmeanything.R;
import com.sumit.askmeanything.Utils;
import com.sumit.askmeanything.model.ResultPod;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Created by sumit on 6/12/2016.
 */
public class ResultPodAdapter extends RecyclerView.Adapter<ResultPodAdapter.ResultPodViewHolder> {

    private List<ResultPod> resultPods;
    private Context context;
    private int lastAnimatedPosition = -1;

    public ResultPodAdapter(List<ResultPod> resultPods) {
        this.resultPods = resultPods;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public ResultPodViewHolder onCreateViewHolder(ViewGroup parent, int i) {

        context = parent.getContext();

        // Set card layout

        View view = LayoutInflater.from(context).inflate(R.layout.result_pod_card_layout, parent, false);
        ResultPodViewHolder resultPodViewHolder = new ResultPodViewHolder(view);
        return resultPodViewHolder;
    }

    @Override
    public void onBindViewHolder(ResultPodViewHolder resultPodViewHolder, final int position) {

        resultPodViewHolder.textViewResultPodTitle.setText(resultPods.get(position).getTitle());
        resultPodViewHolder.textViewResultPodDescription.setText(resultPods.get(position).getDescription());

        // Long tap/press to copy description

        resultPodViewHolder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData;

                if (StringUtils.isNotEmpty(resultPods.get(position).getDescription())) {
                    clipData = ClipData.newPlainText("AskMeAnything", resultPods.get(position).getDescription());
                    clipboardManager.setPrimaryClip(clipData);
                    Toast.makeText(context, "Description copied to clipboard", Toast.LENGTH_LONG).show();
                } else if (StringUtils.isNotEmpty(resultPods.get(position).getImageSource())) {
                    clipData = ClipData.newPlainText("AskMeAnything", resultPods.get(position).getImageSource());
                    clipboardManager.setPrimaryClip(clipData);
                    Toast.makeText(context, "Image link copied to clipboard", Toast.LENGTH_LONG).show();
                }

                return false;
            }
        });

        // Use Fresco to load images Asynchronously

        if (resultPods.get(position).isDefaultCard()) {

            resultPodViewHolder.frescoDraweeViewResultImage.setVisibility(View.VISIBLE);

            // Resource File Example : file:///storage/emulated/0/Pictures/AskMeAnything/IMG_20160617_231541.jpg

            Uri imageUri = null;

            if (StringUtils.containsIgnoreCase(resultPods.get(position).getImageSource(), "file:///storage/")) {

                // Load picture taken

                imageUri = Uri.parse(resultPods.get(position).getImageSource());

                resultPodViewHolder.frescoDraweeViewResultImage.getHierarchy()
                        .setActualImageScaleType(ScalingUtils.ScaleType.CENTER_CROP);

            } else {

                // Show Title

                resultPodViewHolder.textViewResultPodTitle.setVisibility(View.VISIBLE);

                // Load wink image using Fresco

                imageUri = new Uri.Builder()
                        .scheme(UriUtil.LOCAL_RESOURCE_SCHEME) // "res"
                        .path(String.valueOf(R.drawable.ic_wink))
                        .build();

                resultPodViewHolder.frescoDraweeViewResultImage.getHierarchy()
                        .setActualImageScaleType(ScalingUtils.ScaleType.CENTER_INSIDE);

                // Added padding to have some spacing between title, image and description

                resultPodViewHolder.frescoDraweeViewResultImage.setPadding(0, 20, 0, 20);
            }

            resultPodViewHolder.frescoDraweeViewResultImage.setImageURI(imageUri);

        } else if (StringUtils.isNotEmpty(resultPods.get(position).getImageSource())) {

            // Fetch image from URL
            // Hide Title and Description

            resultPodViewHolder.textViewResultPodTitle.setVisibility(View.GONE);
            resultPodViewHolder.textViewResultPodDescription.setVisibility(View.GONE);

            // Keep Fresco View Visible

            resultPodViewHolder.frescoDraweeViewResultImage.setVisibility(View.VISIBLE);

            Uri imageUri = Uri.parse(resultPods.get(position).getImageSource());

            // Enable .gif support

            DraweeController controller = Fresco.newDraweeControllerBuilder()
                    .setUri(imageUri)
                    .setAutoPlayAnimations(true)
                    .build();

            // Set padding to 0 as no border is needed. Show full image card.

            resultPodViewHolder.frescoDraweeViewResultImage.setPadding(0, 0, 0, 0);

            resultPodViewHolder.frescoDraweeViewResultImage.getHierarchy()
                    .setProgressBarImage(new ProgressBarDrawable());
            resultPodViewHolder.frescoDraweeViewResultImage.getHierarchy()
                    .setActualImageScaleType(ScalingUtils.ScaleType.CENTER_CROP);
            resultPodViewHolder.frescoDraweeViewResultImage
                    .setController(controller);

        } else {

            // Do not show Fresco Image View

            resultPodViewHolder.frescoDraweeViewResultImage.setVisibility(View.GONE);
        }

        // Set Animation

        setAnimation(resultPodViewHolder.cardView, position);
    }

    @Override
    public int getItemCount() {
        return resultPods.size();
    }


    // Here is the key method to apply the animation

    private void setAnimation(View viewToAnimate, int position) {

        viewToAnimate.clearAnimation();

        // If the bound view wasn't previously displayed on screen, it's animated

        if (position > lastAnimatedPosition) {
            lastAnimatedPosition = position;
            viewToAnimate.setTranslationY(Utils.getScreenHeight(context));
            viewToAnimate.animate()
                    .translationY(0)
                    .setInterpolator(new DecelerateInterpolator(3.f))
                    .setDuration(700)
                    .start();
        } else return;

    }

    public static class ResultPodViewHolder extends RecyclerView.ViewHolder {

        CardView cardView;
        TextView textViewResultPodTitle;
        TextView textViewResultPodDescription;
        SimpleDraweeView frescoDraweeViewResultImage;

        ResultPodViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView.findViewById(R.id.result_pod_card_view);
            textViewResultPodTitle = (TextView) itemView.findViewById(R.id.text_result_pod_title);
            textViewResultPodDescription = (TextView) itemView.findViewById(R.id.text_result_pod_description);
            frescoDraweeViewResultImage = (SimpleDraweeView) itemView.findViewById(R.id.image_result_pod);
        }
    }
}
