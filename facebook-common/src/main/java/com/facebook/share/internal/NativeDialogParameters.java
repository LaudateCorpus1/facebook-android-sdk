/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.share.internal;

import android.os.Bundle;
import androidx.annotation.Nullable;
import com.facebook.FacebookException;
import com.facebook.internal.Utility;
import com.facebook.internal.Validate;
import com.facebook.internal.qualityvalidation.Excuse;
import com.facebook.internal.qualityvalidation.ExcusesForDesignViolations;
import com.facebook.share.model.ShareCameraEffectContent;
import com.facebook.share.model.ShareContent;
import com.facebook.share.model.ShareHashtag;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.ShareMediaContent;
import com.facebook.share.model.ShareMessengerGenericTemplateContent;
import com.facebook.share.model.ShareMessengerMediaTemplateContent;
import com.facebook.share.model.ShareMessengerOpenGraphMusicTemplateContent;
import com.facebook.share.model.ShareOpenGraphContent;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.model.ShareStoryContent;
import com.facebook.share.model.ShareVideoContent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * com.facebook.share.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
@ExcusesForDesignViolations(@Excuse(type = "MISSING_UNIT_TEST", reason = "Legacy"))
public class NativeDialogParameters {

  public static Bundle create(
      UUID callId, ShareContent shareContent, boolean shouldFailOnDataError) {
    Validate.notNull(shareContent, "shareContent");
    Validate.notNull(callId, "callId");

    Bundle nativeParams = null;
    if (shareContent instanceof ShareLinkContent) {
      final ShareLinkContent linkContent = (ShareLinkContent) shareContent;
      nativeParams = create(linkContent, shouldFailOnDataError);
    } else if (shareContent instanceof SharePhotoContent) {
      final SharePhotoContent photoContent = (SharePhotoContent) shareContent;
      List<String> photoUrls = ShareInternalUtility.getPhotoUrls(photoContent, callId);

      nativeParams = create(photoContent, photoUrls, shouldFailOnDataError);
    } else if (shareContent instanceof ShareVideoContent) {
      final ShareVideoContent videoContent = (ShareVideoContent) shareContent;
      String videoUrl = ShareInternalUtility.getVideoUrl(videoContent, callId);

      nativeParams = create(videoContent, videoUrl, shouldFailOnDataError);
    } else if (shareContent instanceof ShareOpenGraphContent) {
      final ShareOpenGraphContent openGraphContent = (ShareOpenGraphContent) shareContent;
      try {
        JSONObject openGraphActionJSON =
            ShareInternalUtility.toJSONObjectForCall(callId, openGraphContent);
        openGraphActionJSON =
            ShareInternalUtility.removeNamespacesFromOGJsonObject(openGraphActionJSON, false);
        nativeParams = create(openGraphContent, openGraphActionJSON, shouldFailOnDataError);
      } catch (final JSONException e) {
        throw new FacebookException(
            "Unable to create a JSON Object from the provided ShareOpenGraphContent: "
                + e.getMessage());
      }
    } else if (shareContent instanceof ShareMediaContent) {
      final ShareMediaContent mediaContent = (ShareMediaContent) shareContent;
      List<Bundle> mediaInfos = ShareInternalUtility.getMediaInfos(mediaContent, callId);

      nativeParams = create(mediaContent, mediaInfos, shouldFailOnDataError);
    } else if (shareContent instanceof ShareCameraEffectContent) {
      final ShareCameraEffectContent cameraEffectContent = (ShareCameraEffectContent) shareContent;

      // Put Bitmaps behind content uris.
      Bundle attachmentUrlsBundle =
          ShareInternalUtility.getTextureUrlBundle(cameraEffectContent, callId);

      nativeParams = create(cameraEffectContent, attachmentUrlsBundle, shouldFailOnDataError);
    } else if (shareContent instanceof ShareMessengerGenericTemplateContent) {
      final ShareMessengerGenericTemplateContent genericTemplateContent =
          (ShareMessengerGenericTemplateContent) shareContent;
      nativeParams = create(genericTemplateContent, shouldFailOnDataError);
    } else if (shareContent instanceof ShareMessengerOpenGraphMusicTemplateContent) {
      final ShareMessengerOpenGraphMusicTemplateContent openGraphMusicTemplateContent =
          (ShareMessengerOpenGraphMusicTemplateContent) shareContent;
      nativeParams = create(openGraphMusicTemplateContent, shouldFailOnDataError);
    } else if (shareContent instanceof ShareMessengerMediaTemplateContent) {
      final ShareMessengerMediaTemplateContent mediaTemplateContent =
          (ShareMessengerMediaTemplateContent) shareContent;
      nativeParams = create(mediaTemplateContent, shouldFailOnDataError);
    } else if (shareContent instanceof ShareStoryContent) {
      final ShareStoryContent storyContent = (ShareStoryContent) shareContent;
      Bundle mediaInfo = ShareInternalUtility.getBackgroundAssetMediaInfo(storyContent, callId);
      Bundle stickerInfo = ShareInternalUtility.getStickerUrl(storyContent, callId);
      nativeParams = create(storyContent, mediaInfo, stickerInfo, shouldFailOnDataError);
    }

    return nativeParams;
  }

  private static Bundle create(
      ShareCameraEffectContent cameraEffectContent,
      Bundle attachmentUrlsBundle,
      boolean dataErrorsFatal) {
    Bundle params = createBaseParameters(cameraEffectContent, dataErrorsFatal);

    Utility.putNonEmptyString(params, ShareConstants.EFFECT_ID, cameraEffectContent.getEffectId());

    if (attachmentUrlsBundle != null) {
      params.putBundle(ShareConstants.EFFECT_TEXTURES, attachmentUrlsBundle);
    }

    try {
      JSONObject argsJSON =
          CameraEffectJSONUtility.convertToJSON(cameraEffectContent.getArguments());
      if (argsJSON != null) {
        Utility.putNonEmptyString(params, ShareConstants.EFFECT_ARGS, argsJSON.toString());
      }
    } catch (JSONException e) {
      throw new FacebookException(
          "Unable to create a JSON Object from the provided CameraEffectArguments: "
              + e.getMessage());
    }

    return params;
  }

  private static Bundle create(ShareLinkContent linkContent, boolean dataErrorsFatal) {
    Bundle params = createBaseParameters(linkContent, dataErrorsFatal);

    Utility.putNonEmptyString(params, ShareConstants.TITLE, linkContent.getContentTitle());
    Utility.putNonEmptyString(
        params, ShareConstants.DESCRIPTION, linkContent.getContentDescription());
    Utility.putUri(params, ShareConstants.IMAGE_URL, linkContent.getImageUrl());
    Utility.putNonEmptyString(params, ShareConstants.QUOTE, linkContent.getQuote());
    Utility.putUri(params, ShareConstants.MESSENGER_URL, linkContent.getContentUrl());
    Utility.putUri(params, ShareConstants.TARGET_DISPLAY, linkContent.getContentUrl());

    return params;
  }

  private static Bundle create(
      SharePhotoContent photoContent, List<String> imageUrls, boolean dataErrorsFatal) {
    Bundle params = createBaseParameters(photoContent, dataErrorsFatal);

    params.putStringArrayList(ShareConstants.PHOTOS, new ArrayList<>(imageUrls));

    return params;
  }

  private static Bundle create(
      ShareVideoContent videoContent, String videoUrl, boolean dataErrorsFatal) {
    Bundle params = createBaseParameters(videoContent, dataErrorsFatal);

    Utility.putNonEmptyString(params, ShareConstants.TITLE, videoContent.getContentTitle());
    Utility.putNonEmptyString(
        params, ShareConstants.DESCRIPTION, videoContent.getContentDescription());
    Utility.putNonEmptyString(params, ShareConstants.VIDEO_URL, videoUrl);

    return params;
  }

  private static Bundle create(
      ShareMediaContent mediaContent, List<Bundle> mediaInfos, boolean dataErrorsFatal) {
    Bundle params = createBaseParameters(mediaContent, dataErrorsFatal);

    params.putParcelableArrayList(ShareConstants.MEDIA, new ArrayList<>(mediaInfos));

    return params;
  }

  private static Bundle create(
      ShareOpenGraphContent openGraphContent,
      JSONObject openGraphActionJSON,
      boolean dataErrorsFatal) {
    Bundle params = createBaseParameters(openGraphContent, dataErrorsFatal);

    // Strip namespace from preview property name
    String previewProperty =
        ShareInternalUtility.getFieldNameAndNamespaceFromFullName(
                openGraphContent.getPreviewPropertyName())
            .second;

    Utility.putNonEmptyString(params, ShareConstants.PREVIEW_PROPERTY_NAME, previewProperty);
    Utility.putNonEmptyString(
        params, ShareConstants.ACTION_TYPE, openGraphContent.getAction().getActionType());

    Utility.putNonEmptyString(params, ShareConstants.ACTION, openGraphActionJSON.toString());

    return params;
  }

  private static Bundle create(
      ShareMessengerGenericTemplateContent genericTemplateContent, boolean dataErrorsFatal) {
    Bundle params = createBaseParameters(genericTemplateContent, dataErrorsFatal);

    try {
      MessengerShareContentUtility.addGenericTemplateContent(params, genericTemplateContent);
    } catch (JSONException e) {
      throw new FacebookException(
          "Unable to create a JSON Object from the provided "
              + "ShareMessengerGenericTemplateContent: "
              + e.getMessage());
    }
    return params;
  }

  private static Bundle create(
      ShareMessengerOpenGraphMusicTemplateContent openGraphMusicTemplateContent,
      boolean dataErrorsFatal) {
    Bundle params = createBaseParameters(openGraphMusicTemplateContent, dataErrorsFatal);

    try {
      MessengerShareContentUtility.addOpenGraphMusicTemplateContent(
          params, openGraphMusicTemplateContent);
    } catch (JSONException e) {
      throw new FacebookException(
          "Unable to create a JSON Object from the provided "
              + "ShareMessengerOpenGraphMusicTemplateContent: "
              + e.getMessage());
    }
    return params;
  }

  private static Bundle create(
      ShareMessengerMediaTemplateContent mediaTemplateContent, boolean dataErrorsFatal) {
    Bundle params = createBaseParameters(mediaTemplateContent, dataErrorsFatal);

    try {
      MessengerShareContentUtility.addMediaTemplateContent(params, mediaTemplateContent);
    } catch (JSONException e) {
      throw new FacebookException(
          "Unable to create a JSON Object from the provided "
              + "ShareMessengerMediaTemplateContent: "
              + e.getMessage());
    }

    return params;
  }

  private static Bundle create(
      ShareStoryContent storyContent,
      @Nullable Bundle mediaInfo,
      @Nullable Bundle stickerInfo,
      boolean dataErrorsFatal) {
    Bundle params = createBaseParameters(storyContent, dataErrorsFatal);

    if (mediaInfo != null) {
      params.putParcelable(ShareConstants.STORY_BG_ASSET, mediaInfo);
    }

    if (stickerInfo != null) {
      params.putParcelable(ShareConstants.STORY_INTERACTIVE_ASSET_URI, stickerInfo);
    }

    List<String> backgroundColorList = storyContent.getBackgroundColorList();
    if (!Utility.isNullOrEmpty(backgroundColorList)) {
      params.putStringArrayList(
          ShareConstants.STORY_INTERACTIVE_COLOR_LIST, new ArrayList<>(backgroundColorList));
    }

    Utility.putNonEmptyString(
        params, ShareConstants.STORY_DEEP_LINK_URL, storyContent.getAttributionLink());

    return params;
  }

  private static Bundle createBaseParameters(ShareContent content, boolean dataErrorsFatal) {
    Bundle params = new Bundle();
    Utility.putUri(params, ShareConstants.CONTENT_URL, content.getContentUrl());
    Utility.putNonEmptyString(params, ShareConstants.PLACE_ID, content.getPlaceId());
    Utility.putNonEmptyString(params, ShareConstants.PAGE_ID, content.getPageId());
    Utility.putNonEmptyString(params, ShareConstants.REF, content.getRef());

    params.putBoolean(ShareConstants.DATA_FAILURES_FATAL, dataErrorsFatal);

    List<String> peopleIds = content.getPeopleIds();
    if (!Utility.isNullOrEmpty(peopleIds)) {
      params.putStringArrayList(ShareConstants.PEOPLE_IDS, new ArrayList<>(peopleIds));
    }

    ShareHashtag shareHashtag = content.getShareHashtag();
    if (shareHashtag != null) {
      Utility.putNonEmptyString(params, ShareConstants.HASHTAG, shareHashtag.getHashtag());
    }

    return params;
  }
}
