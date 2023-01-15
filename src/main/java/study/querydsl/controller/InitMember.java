package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

//application.yml 참고 이건 src의 application.yml이 있고 test 폴더 아래 application.yml의 profile은 test이다.
@Profile("local")
//스프링 빈 등록
@Component
@RequiredArgsConstructor
public class InitMember {

    private final InitMemberService initMemberService;

    //트랜젝션 부분을 분리해줘야 한다 왜냐면 PostConstruct와 Transactional 어노테이션을 같이
    //쓰지 못하기 때문이다.
    @PostConstruct
    public void init(){
        initMemberService.init();
    }

    @Component
    static class InitMemberService{
        @PersistenceContext
        private EntityManager em;

        @Transactional
        public void init(){
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");

            em.persist(teamA);
            em.persist(teamB);

            for (int i = 0; i < 100; i++) {
                Team selectedTeam = i % 2 == 0 ? teamA :teamB;
                em.persist(new Member("member"+i, i, selectedTeam));
            }
        }
    }
}
