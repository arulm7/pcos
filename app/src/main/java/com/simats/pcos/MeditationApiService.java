package com.simats.pcos;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface MeditationApiService {

    @FormUrlEncoded
    @POST("meditation.php")
    Call<MeditationResponse> fetchMeditationAudio(@Field("category") String category);

    @POST("store_answers.php")
    Call<CommonResponse> storeAnswer(@Body QuestionAndAnswers1.StoreAnswers storeAnswers);

    @GET("fetch_user_given_answers.php")
    Call<PatientAnswersActivity.AnswersResponse> getAnswers(@Query("username") String username);


}
