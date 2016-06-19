package com.sumit.askmeanything.adapter;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.common.util.UriUtil;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.ProgressBarDrawable;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.sumit.askmeanything.R;
import com.sumit.askmeanything.model.ResultPod;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Created by sumit on 6/12/2016.
 */
public class ResultPodAdapter extends RecyclerView.Adapter<ResultPodAdapter.ResultPodViewHolder> {

    List<ResultPod> resultPods;
    private Context context;

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
    public void onBindViewHolder(ResultPodViewHolder resultPodViewHolder, int i) {
        resultPodViewHolder.textViewResultPodTitle.setText(resultPods.get(i).getTitle());
        resultPodViewHolder.textViewResultPodDescription.setText(resultPods.get(i).getDescription());

        // Use Fresco to load images Asynchronously

        if (resultPods.get(i).isDefaultCard()) {

            resultPodViewHolder.frescoDraweeViewResultImage.setVisibility(View.VISIBLE);

            // Resource File Example : file:///storage/emulated/0/Pictures/AskMeAnything/IMG_20160617_231541.jpg

            Uri imageUri = null;

            if (StringUtils.containsIgnoreCase(resultPods.get(i).getImageSource(), "file:///storage/")) {

                // Load picture taken

                imageUri = Uri.parse(resultPods.get(i).getImageSource());

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

            resultPodViewHolder.frescoDraweeViewResultImage.getHierarchy()
                    .setProgressBarImage(new ProgressBarDrawable());

            resultPodViewHolder.frescoDraweeViewResultImage.setImageURI(imageUri);

        } else if (StringUtils.isNotEmpty(resultPods.get(i).getImageSource())) {

            // Fetch image from URL
            // Hide Title and Description

            resultPodViewHolder.textViewResultPodTitle.setVisibility(View.GONE);
            resultPodViewHolder.textViewResultPodDescription.setVisibility(View.GONE);

            // Keep Fresco View Visible

            resultPodViewHolder.frescoDraweeViewResultImage.setVisibility(View.VISIBLE);

            Uri imageUri = Uri.parse(resultPods.get(i).getImageSource());

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
    }

    @Override
    public int getItemCount() {
        return resultPods.size();
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
