package org.sagebionetworks.web.client.place.users;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class PasswordReset extends Place {

  private String token;

  public PasswordReset(String token) {
    this.token = token;
  }

  public String toToken() {
    return token;
  }

  @Prefix("PasswordReset")
  public static class Tokenizer implements PlaceTokenizer<PasswordReset> {

    @Override
    public String getToken(PasswordReset place) {
      return place.toToken();
    }

    @Override
    public PasswordReset getPlace(String token) {
      return new PasswordReset(token);
    }
  }
}
