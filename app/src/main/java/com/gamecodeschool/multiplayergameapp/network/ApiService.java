package com.gamecodeschool.multiplayergameapp.network;

import com.gamecodeschool.multiplayergameapp.models.Lobby;
import com.gamecodeschool.multiplayergameapp.models.LobbyResponse;
import com.gamecodeschool.multiplayergameapp.models.ResponseBody;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    @FormUrlEncoded
    @POST("/register")
    Call<ResponseBody> register(
            @Field("username") String username,
            @Field("password") String password,
            @Field("password2") String password2
    );

    @FormUrlEncoded
    @POST("/login")
    Call<ResponseBody> login(
            @Field("username") String username,
            @Field("password") String password
    );

    @GET("/get-lobbies")
    Call<LobbyResponse> getAvailableLobbies();

    @FormUrlEncoded
    @POST("/create-lobby")
    Call<ResponseBody> createGameSession(
            @Field("username") String username,
            @Field("gameType") String gameType
    );

    @GET("/fetch-lobby-details")
    Call<Lobby> getLobbyDetails(
            @Query("username") String username
    );

    @FormUrlEncoded
    @POST("/delete-lobby")
    Call<ResponseBody> deleteLobby(
            @Field("lobbyId") int lobbyId
    );

    @FormUrlEncoded
    @POST("/lobby/join")
    Call<ResponseBody> joinLobby(
            @Field("lobbyId") int lobbyId,
            @Field("username") String username
    );

    @GET("/get-username-by-id")
    Call<String> getUserIdToUsername(
            @Query("userId") int userId
    );
}
