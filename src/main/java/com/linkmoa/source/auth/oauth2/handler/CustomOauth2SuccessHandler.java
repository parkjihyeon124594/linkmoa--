package com.linkmoa.source.auth.oauth2.handler;


import com.linkmoa.source.auth.jwt.refresh.service.RefreshTokenService;
import com.linkmoa.source.auth.jwt.service.JwtService;
import com.linkmoa.source.auth.oauth2.principal.PrincipalDetails;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOauth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    //SimpleUrlAuthenticationSuccessHandler는 인증 성공 후 처리를 담당하는 핸들러


    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;


    /**
     *
     *  CustomOAuth2UserService의 loadUser 메서드에서 Principal 객체를 반환하면,
     *  이 객체가 Authentication 객체에 저장됨.
     *  더 자세히 설명하면, OAuth2 로그인 과정에서 loadUser 메서드가 반환하는 객체는 Authentication 객체의 Principal로 설정됨.
     *
     */

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        PrincipalDetails oauth2User = (PrincipalDetails) authentication.getPrincipal();
        Collection<? extends GrantedAuthority> authorities = oauth2User.getAuthorities();

        String email = oauth2User.getEmail();
        String role = authorities.iterator().next().getAuthority();


        String refreshToken =  jwtService.createRefreshToken();
        refreshTokenService.saveRefreshToken(email,refreshToken);
        response.addCookie(jwtService.createCookie("refresh_token", refreshToken));

        response.sendRedirect("http://localhost:5173/reissue");


        // 테스트용으로 추가한 부분
        String accessToken =jwtService.createAccessToken(email,role);
        response.addCookie(jwtService.createCookie("access_token", accessToken));
        response.setHeader("access_token",accessToken);


        log.info("OAuth2 로그인에 성공 하였습니다. access Token : {}",accessToken);
        log.info("OAuth2 로그인에 성공하였습니다. 이메일 : {}",  oauth2User.getEmail());
        log.info("OAuth2 로그인에 성공하였습니다. Refresh Token : {}",  refreshToken);

        /**
         * 자체 회원가입 로직
         * 1.프론트 OAuth2 소셜 로그인 후 쿠키에 리프레쉬 토큰만 응답
         * 2-1. 새로운 유저
         *  - 서버에서 redirect /reissue
         *  - 클라이언트에서 api(엑세스 토큰 요청) 호출
         *  - 응답으로 헤더에 엑세스 토큰, 쿠키에 리프레쉬 토큰 - status 200, 201나 다른 필드로 구분하는 등 
         *  - 클라이언트에서 사인업 페이지로 리다이렉트
         *
         * 2-2. 기존 유저
         *  - 서버에서 redirect /reissue
         *  - 클라이언트에서 api(엑세스 토큰 요청) 호출
         *  - 응답으로 헤더에 엑세스 토큰, 쿠키에 리프레쉬 토큰
         *  - 클라이언트에서 메인 페이지로 리다이렉
         */

    }
}
