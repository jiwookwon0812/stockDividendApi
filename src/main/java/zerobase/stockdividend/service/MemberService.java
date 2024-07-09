package zerobase.stockdividend.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import zerobase.stockdividend.exception.impl.AlreadyExistUserException;
import zerobase.stockdividend.model.Auth;
import zerobase.stockdividend.persist.entity.MemberEntity;
import zerobase.stockdividend.persist.repository.MemberRepository;

@Slf4j
@Service
@AllArgsConstructor
public class MemberService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("could not find user -> " + username));
    }

    // 회원가입
    public MemberEntity register(Auth.SignUp member) {
        boolean exists = memberRepository.existsByUsername(member.getUsername());
        if (exists) {
            throw new AlreadyExistUserException();
        }

        member.setPassword(passwordEncoder.encode(member.getPassword()));
        var result = memberRepository.save(member.toEntity());


        return result;
    }

    // 로그인 인증 (패스워드 인증)
    public MemberEntity authenticate(Auth.SignIn member) {
        // id 존재하는지 확인
        var user = memberRepository.findByUsername(member.getUsername())
                .orElseThrow(() -> new RuntimeException("could not find user -> " + member.getUsername()));

        // id 와 비밀번호 일치하는지 확인
        if (!passwordEncoder.matches(member.getPassword(), user.getPassword())) {
            throw new RuntimeException("password does not match");
        }
        return user;
    }

}
