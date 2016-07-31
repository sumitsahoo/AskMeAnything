package com.sumit.askmeanything.parser;

import android.net.Uri;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sumit.askmeanything.model.ResultPod;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by sumit on 6/14/2016.
 */
public class CognitiveApiResponseJsonParser {

    public static ArrayList<String> parseImageUrlFromResponseJson(JsonObject jsonObject) {

        ArrayList<String> imageUrls = null;

        try {

            JsonArray jsonValueArray = jsonObject.getAsJsonArray("value");

            if (jsonValueArray != null && jsonValueArray.size() > 0) {

                imageUrls = new ArrayList<>();

                for (JsonElement resultObject : jsonValueArray) {
                    imageUrls.add(((JsonObject) resultObject).get("contentUrl").getAsString());
                }
            }

            return imageUrls;
        } catch (Exception e) {
            return null;
        }
    }

    public static List<ResultPod> parseImageDescriptionFromResponseJson(JsonObject jsonObject, Uri imageFileUri) {

        String imageDescription = null;
        String tags = null;

        String celebrityName = null;

        int age = -1;
        String gender = null;

        String dominantColors = null;
        boolean isBWImage = false;

        try {

            // Description

            JsonObject descriptionObject = jsonObject.getAsJsonObject("description");
            JsonObject captionObject = (JsonObject) descriptionObject.getAsJsonArray("captions").get(0);

            imageDescription = captionObject.get("text").getAsString();

            // Celebrity Name

            JsonArray celebrityArray = jsonObject.getAsJsonArray("categories");

            if (celebrityArray != null && celebrityArray.size() > 0) {
                JsonObject celebrityObject = celebrityArray.get(0).getAsJsonObject().getAsJsonObject("detail");

                if (celebrityObject != null) {
                    try {
                        celebrityName = ((JsonObject) celebrityObject.getAsJsonArray("celebrities").get(0)).get("name").getAsString();
                    } catch (IndexOutOfBoundsException e) {
                        celebrityName = null;
                    }
                }
            }

            // Faces : Age and Gender

            JsonArray faceArray = jsonObject.getAsJsonArray("faces");

            if (faceArray != null && faceArray.size() > 0) {
                JsonObject faceObject = (JsonObject) faceArray.get(0);

                if (faceObject.get("age") != null)
                    age = faceObject.get("age").getAsInt();

                if (faceObject.get("gender") != null)
                    gender = faceObject.get("gender").getAsString();
            }

            // Dominant Colors

            JsonObject colorObject = jsonObject.getAsJsonObject("color");

            if (colorObject != null) {
                JsonArray colorArray = colorObject.getAsJsonArray("dominantColors");

                if (colorArray != null && colorArray.size() > 0) {
                    for (JsonElement dominantColorElement : colorArray) {
                        if (dominantColors == null) {
                            dominantColors = dominantColorElement.getAsString();
                        } else {
                            dominantColors += ", " + dominantColorElement.getAsString();
                        }
                    }
                }
            }

            // Tags

            JsonArray tagsArray = descriptionObject.getAsJsonArray("tags");

            for (JsonElement tagElement : tagsArray) {
                if (tags == null) {
                    tags = tagElement.getAsString();
                } else tags += ", " + tagElement.getAsString();
            }

            // Prepare full image description

            List<ResultPod> resultPods = new ArrayList<>();

            ResultPod descriptionCard = new ResultPod();
            descriptionCard.setDescription(StringUtils.capitalize(imageDescription));
            descriptionCard.setTitle("Image Description");
            descriptionCard.setDefaultCard(true);
            descriptionCard.setImageSource(imageFileUri.toString());

            ResultPod personCard = new ResultPod();
            personCard.setDefaultCard(false);
            personCard.setTitle("Person Details");

            String personDetails = StringUtils.isNotEmpty(celebrityName) ? "Name : " + celebrityName : "";
            if (StringUtils.isNotEmpty(celebrityName))
                personDetails += StringUtils.isNotEmpty(gender) ? "\nDetected Gender : " + gender : "";
            else
                personDetails = StringUtils.isNotEmpty(gender) ? "Detected Gender : " + gender : "";
            personDetails += age != -1 ? "\nDetected Age : " + age : "";

            personCard.setDescription(personDetails);

            ResultPod colorCard = new ResultPod();
            colorCard.setTitle("Dominant Colors");
            colorCard.setDescription(dominantColors);
            colorCard.setDefaultCard(false);

            ResultPod tagCard = new ResultPod();
            tagCard.setTitle("Image Tags");
            tagCard.setDefaultCard(false);
            tagCard.setDescription(tags);

            // Add results to ArrayList

            resultPods.add(descriptionCard);

            if (StringUtils.isNotEmpty(celebrityName) || StringUtils.isNotEmpty(gender) || age != -1)
                resultPods.add(personCard);

            if (StringUtils.isNotEmpty(dominantColors))
                resultPods.add(colorCard);

            if (StringUtils.isNotEmpty(tags))
                resultPods.add(tagCard);

            return resultPods;
        } catch (Exception e) {
            return null;
        }
    }

    // Parse OCR Response JSON

    public static List<ResultPod> parseOcrTextFromResponseJson(JsonObject jsonObject, Uri imageFileUri) {

        ArrayList<ResultPod> resultPods = null;
        String ocrResponseText = null;

        try {
            JsonArray regionArray = jsonObject.getAsJsonArray("regions");

            if (regionArray != null && regionArray.size() > 0) {
                for (JsonElement regionObject : regionArray) {
                    JsonArray linesArray = ((JsonObject) regionObject).getAsJsonArray("lines");

                    if (linesArray != null && linesArray.size() > 0) {
                        for (JsonElement lineObject : linesArray) {
                            JsonArray wordsArray = ((JsonObject) lineObject).getAsJsonArray("words");

                            if (wordsArray != null && wordsArray.size() > 0) {

                                int wordCount = 0;

                                for (JsonElement wordObject : wordsArray) {

                                    if (StringUtils.isEmpty(ocrResponseText)) {
                                        ocrResponseText = ((JsonObject) wordObject).get("text").getAsString();
                                    } else {
                                        if (wordCount == 0)
                                            ocrResponseText += ((JsonObject) wordObject).get("text").getAsString();
                                        else
                                            ocrResponseText += " " + ((JsonObject) wordObject).get("text").getAsString();
                                    }

                                    wordCount++;
                                }
                            }

                            ocrResponseText += "\n";
                        }
                    }
                }
            }

            if (StringUtils.isNotEmpty(ocrResponseText)) {
                ResultPod resultPod = new ResultPod();
                resultPod.setDefaultCard(true);
                resultPod.setTitle("OCR Text");
                resultPod.setImageSource(imageFileUri.toString());
                resultPod.setDescription(ocrResponseText);

                resultPods = new ArrayList<>();
                resultPods.add(resultPod);
            }

            return resultPods;
        } catch (Exception e) {
            return null;
        }
    }

    public static List<ResultPod> parseEmotionFromResponseJson(JsonArray responseArray, Uri imageFileUri) {

        ArrayList<ResultPod> resultPods = null;

        Map<String, String> emotionMap = null;
        ArrayList<Map> emotionList = null;

        try {

            if (responseArray != null && responseArray.size() > 0) {

                emotionMap = new TreeMap<>();
                emotionList = new ArrayList<>();

                for (JsonElement emotionObject : responseArray) {
                    JsonObject emotionScore = ((JsonObject) emotionObject).getAsJsonObject("scores");

                    emotionMap.put("Anger", emotionScore.get("anger").getAsString());
                    emotionMap.put("Contempt", emotionScore.get("contempt").getAsString());
                    emotionMap.put("Disgust", emotionScore.get("disgust").getAsString());
                    emotionMap.put("Fear", emotionScore.get("fear").getAsString());
                    emotionMap.put("Happiness", emotionScore.get("happiness").getAsString());
                    emotionMap.put("Neutral", emotionScore.get("neutral").getAsString());
                    emotionMap.put("Sadness", emotionScore.get("sadness").getAsString());
                    emotionMap.put("Surprise", emotionScore.get("surprise").getAsString());

                    emotionList.add(emotionMap);
                }
            }

            if (emotionList != null && emotionList.size() > 0) {
                ResultPod resultPodImage = new ResultPod();
                resultPodImage.setDefaultCard(false);
                resultPodImage.setImageSource(imageFileUri.toString());
                resultPodImage.setTitle("Detected Emotion (Left To Right)");

                resultPods = new ArrayList<>();
                resultPods.add(resultPodImage);

                int count = 1;

                // Loop through if there are multiple persons in a single image file

                for (Map<String, String> emotion : emotionList) {
                    ResultPod resultPodEmotion = new ResultPod();
                    resultPodEmotion.setDefaultCard(false);
                    resultPodEmotion.setTitle("Detected Emotion (Person #" + count + ")");

                    String emotionDescription = "Anger : " + emotion.get("Anger");
                    emotionDescription += "\nContempt : " + emotion.get("Contempt");
                    emotionDescription += "\nDisgust : " + emotion.get("Disgust");
                    emotionDescription += "\nFear : " + emotion.get("Fear");
                    emotionDescription += "\nHappiness : " + emotion.get("Happiness");
                    emotionDescription += "\nNeutral : " + emotion.get("Neutral");
                    emotionDescription += "\nSadness : " + emotion.get("Sadness");
                    emotionDescription += "\nSurprise : " + emotion.get("Surprise");

                    resultPodEmotion.setDescription(emotionDescription);
                    resultPods.add(resultPodEmotion);

                    count++;
                }

            }

            return resultPods;

        } catch (Exception e) {
            return null;
        }
    }
}
