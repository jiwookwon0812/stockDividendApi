package zerobase.stockdividend.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zerobase.stockdividend.model.Auth;
import zerobase.stockdividend.security.TokenProvider;
import zerobase.stockdividend.service.MemberService;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final MemberService memberService;
    private final TokenProvider tokenProvider;

    @PostMapping("/signup") // 회원가입을 위한 api
    public ResponseEntity<?> signup(@RequestBody Auth.SignUp request) {
        var result = memberService.register(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/signin") // 로그인용 api
    public ResponseEntity<?> signin(@RequestBody Auth.SignIn request) {
        // id 와 비밀번호 검증
        var member = memberService.authenticate(request);
        // 토큰 생성해주기
        var token = tokenProvider.generateToken(member.getUsername(), member.getRoles());
        log.info("user login -> " + request.getUsername());
        return ResponseEntity.ok(token);
    }
}
