package com.segment.analytics.android.integrations.moengage;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import com.moe.pushlibrary.MoEHelper;
import com.moe.pushlibrary.models.GeoLocation;
import com.moe.pushlibrary.utils.MoEHelperConstants;
import com.moengage.core.ConfigurationProvider;
import com.moengage.core.Logger;
import com.segment.analytics.Analytics;
import com.segment.analytics.AnalyticsContext;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.TrackPayload;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.segment.analytics.internal.Utils.isNullOrEmpty;
import static com.segment.analytics.internal.Utils.transform;

/**
 * MoEngage is an advanced mobile marketing and engagement tool which has a wide range of
 * features. It helps user retention and increases churn.
 *
 * @see <a href="http://www.moengage.com/">MoEngage</a>
 * @see <a href="https://segment.com/docs/integrations/moengage/">MoEngage Integration</a>
 * @see <a href="http://docs.moengage.com/en/latest/android.html">MoEngage Android SDK</a>
 */
public class MoEngageIntegration extends Integration<MoEHelper> {
  public static final Factory FACTORY = new Factory() {
    @Override public Integration<?> create(ValueMap settings, Analytics analytics) {
      return new MoEngageIntegration(analytics, settings);
    }

    @Override public String key() {
      return KEY_MOENGAGE;
    }
  };
  private static final String KEY_MOENGAGE = "MoEngage";
  private static final Map<String, String> MAPPER;

  static {
    Map<String, String> mapper = new LinkedHashMap<>();
    mapper.put("anonymousId", "USER_ATTRIBUTE_SEGMENT_ID");
    mapper.put("email", MoEHelperConstants.USER_ATTRIBUTE_USER_EMAIL);
    mapper.put("userId", MoEHelperConstants.USER_ATTRIBUTE_UNIQUE_ID);
    mapper.put("name", MoEHelperConstants.USER_ATTRIBUTE_USER_NAME);
    mapper.put("phone", MoEHelperConstants.USER_ATTRIBUTE_USER_MOBILE);
    mapper.put("firstName", MoEHelperConstants.USER_ATTRIBUTE_USER_FIRST_NAME);
    mapper.put("lastName", MoEHelperConstants.USER_ATTRIBUTE_USER_LAST_NAME);
    mapper.put("gender", MoEHelperConstants.USER_ATTRIBUTE_USER_GENDER);
    mapper.put("birthday", MoEHelperConstants.USER_ATTRIBUTE_USER_BDAY);
    MAPPER = Collections.unmodifiableMap(mapper);
  }

  MoEHelper helper;

  MoEngageIntegration(Analytics analytics, ValueMap settings) throws IllegalStateException {
    Context context = analytics.getApplication();
    String apiKey = settings.getString("apiKey");
    String pushSenderId = settings.getString("pushSenderId");
    helper = MoEHelper.getInstance(context);
    Logger.d("MoEngageIntegration : Segment MoEngage Integration initialized");
    helper.initialize(pushSenderId, apiKey);
    ConfigurationProvider.getInstance(context).setSegmentEnabledFlag(true);
    ConfigurationProvider.getInstance(context)
        .saveSegmentAnonymousId(
            Analytics.with(context).getAnalyticsContext().traits().anonymousId());
  }

  @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    super.onActivityCreated(activity, savedInstanceState);
    helper = new MoEHelper(activity);
    if (savedInstanceState != null) {
      helper.onRestoreInstanceState(savedInstanceState);
    }
  }

  @Override public void onActivityStarted(Activity activity) {
    super.onActivityStarted(activity);
    helper.onStart(activity);
  }


  @Override public void onActivityResumed(Activity activity) {
    super.onActivityResumed(activity);
    helper.onResume(activity);
  }

  @Override public void onActivityPaused(Activity activity) {
    super.onActivityPaused(activity);
  }

  @Override public void onActivityStopped(Activity activity) {
    super.onActivityStopped(activity);
    helper.onStop(activity);
  }

  @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    super.onActivitySaveInstanceState(activity, outState);
    helper.onSaveInstanceState(outState);
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);
    Traits traits = identify.traits();

    if (!isNullOrEmpty(traits)) {
      helper.setUserAttribute(transform(traits, MAPPER));
      Traits.Address address = traits.address();
      if (!isNullOrEmpty(address)) {
        String city = address.city();
        if (!isNullOrEmpty(city)) {
          helper.setUserAttribute("city", city);
        }
        String country = address.country();
        if (!isNullOrEmpty(country)) {
          helper.setUserAttribute("country", country);
        }
        String state = address.state();
        if (!isNullOrEmpty(state)) {
          helper.setUserAttribute("state", state);
        }
      }
    }

    AnalyticsContext.Location location = identify.context().location();
    if (!isNullOrEmpty(location)) {
      helper.setUserAttribute(MoEHelperConstants.USER_ATTRIBUTE_USER_LOCATION,
          new GeoLocation(location.latitude(), location.longitude()));
    }
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    if (!isNullOrEmpty(track)) {
      if (!isNullOrEmpty(track.properties())) {
        helper.trackEvent(track.event(), track.properties().toJsonObject());
      } else {
        helper.trackEvent(track.event());
      }
    }
  }

  @Override public void reset() {
    super.reset();
    helper.logoutUser();
  }

  @Override public MoEHelper getUnderlyingInstance() {
    return helper;
  }
}
