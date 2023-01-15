package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static com.querydsl.jpa.JPAExpressions.selectFrom;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
/**
 * transactional이 없으면 오류가 발생한다
 * JPA는 trasaction을 기반으로 작동하게 되어있다.
 * transaction 단위에 따라 1차 캐시 영역에 있는 객체들이 db에 flush되어 영속화되기 때문이다.
 * 하지만 그러한 영속작업을 하는 것이 `persist() 메서드에 객체가 들어갔으나 가능한 trasaction이 존재하지 않으면 에러가 발생한다.
 */
@Transactional
public class QuerydslBasicTest {
    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() {
        //member1을 찾아라.
        String qlString =
                "select m from Member m " +
                        "where m.username = :username";
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {
        //어떤 Q멤버인지 구분하기 위해서 이름을 줘야 한다.
        QMember m = new QMember("m");
        //QMember m = QMember.member 이렇게 써도 된다.
        //

        /**
         * QueryDsl를 사용하면 컴파일 도중에 오류를 잡아주는 방면 JPQL은 그 해당 메소드를 호출하고 나서야 runtime Error가
         * 발생한다. 그래서 QueryDsl이 오류를 더 빨리 찾을 수 있고 자동으로 파라미터 바인딩을 해주기 때문에 SQL Injection으로부터
         * 안전하다(PreparedStatement) 사용
         */
        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))//파라미터 바인딩 처리
                .fetchOne();

 /* QMember.member를 static import 해서 member만 써서 사용해도 된다.
 이렇게 쓰는 것을 권장한다. 이렇게 하면 깔끔하다.
  Member findMember = queryFactory`
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))//파라미터 바인딩 처리
                .fetchOne();*/
        Member findMember2 = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))//파라미터 바인딩 처리
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member) //select와 from를 합칠 수 있다.
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member) //select와 from를 합칠 수 있다.
                //search()와는 다르게 and를 이런 식으로 풀어서 쓸수도 있다.
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10) //and를 안 쓰고 쉼표를 사용해서 할 수 있다.
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() {
//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch();
//
//        Member fetchOne = queryFactory
//                .selectFrom(QMember.member)
//                .fetchOne();
//
//        Member fetchFrist = queryFactory
//                .selectFrom(QMember.member)
//                .fetchFirst();//limit(1)이랑 같음

        /**
         *  Querydsl fetchResults() , fetchCount() Deprecated(향후 미지원)
         * Querydsl의 fetchCount() , fetchResult() 는 개발자가 작성한 select 쿼리를 기반으로 count용
         * 쿼리를 내부에서 만들어서 실행합니다.
         * 그런데 이 기능은 강의에서 설명드린 것 처럼 select 구문을 단순히 count 처리하는 용도로 바꾸는
         * 정도입니다. 따라서 단순한 쿼리에서는 잘 동작하지만, 복잡한 쿼리에서는 제대로 동작하지 않습니다.
         * Querydsl은 향후 fetchCount() , fetchResult() 를 지원하지 않기로 결정했습니다.
         * 참고로 Querydsl의 변화가 빠르지는 않기 때문에 당장 해당 기능을 제거하지는 않을 것입니다.
         */
/*        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults(); //이것은 results에서 results.getTotal()를 제공  하지만 fetchResults()는 deprecated 됐다
        results.getTotal();
        List<Member> results1 = results.getResults();*/

        /**
         * 따라서 count 쿼리가 필요하면 다음과 같이 별도로 작성해야 한다.
         */

        Long totalCount = queryFactory
                //select(Wildcard.count) // select count(*)
                .select(member.count())
                .from(member)
                .fetchOne();

        System.out.println("totalCount = " + totalCount);

    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast()) //nullsFirst도 있다
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1() {
        queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc()) //페이징에서는 orderBy를 넣어야 제대로 작동한다
                .offset(1) //offset은 0부터 시작이니까 offset(1)하면 하나부터 시작한다는 것이다.
                .limit(2)
                .fetch();
    }

    @Test
    public void paging2() {
        /**
         * deprecated 됨 위에 주석참고하기
         */
        //만약에 전체 조회 결과가 필요하다면 아래와 같이 한다.
/*        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc()) //페이징에서는 orderBy를 넣어야 제대로 작동한다
                .offset(1) //offset은 0부터 시작이니까 offset(1)하면 하나부터 시작한다는 것이다.
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);*/

        Long count = queryFactory
                .select(Wildcard.count)
                .from(member)
                .orderBy(member.username.desc()) //페이징에서는 orderBy를 넣어야 제대로 작동한다
                .limit(2)
                .groupBy(member.age)
                .fetchOne();

        assertThat(count).isEqualTo(1);
    }

    //집합함수에 대해서 알아보자
    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        //Tuple은 Querydsl이 제공하는 Tuple이다 이것은 이런 식으로 데이터를 꺼낸다
        /**
         * 실무에선 Tuple를 많이 쓰진 않고 DTO로 뽑아내는 방식이 있는데 그 방식을 많이 쓴다.
         */
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15); // (10+20) /2

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35); // (30 + 40 ) / 2
    }

    /**
     * 팀 A에 소속된 모든 회원을 찾아라
     */
    /**
     * 주의! 문법을 잘 봐야 한다. leftJoin() 부분에 일반 조인과 다르게 엔티티 하나만 들어간다.
     * 일반조인: leftJoin(member.team, team) - 이건 member와 연관된 값으로 엮는 것이다(foreign key 값으로)
     * on조인: from(member).leftJoin(team).on(xxx)
     */
    @Test
    public void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team) //team은 QTeam.team를 의미 여기선 static import를 함
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인
     * 회원의 이름이 팀 이름과 같은 회원을 조회
     */
    @Test
    public void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        /*
        - from 절에 여러 엔티티를 선택해서 세타 조인
        - 외부 조인 불가능 다음에 설명할 조인 on을 사용하면 외부 조인 가능
        */
        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    public void join_on_filtering() {

//        List<Tuple> result = queryFactory
//                .select(member, team)
//                .from(member)
//                .leftJoin(member.team, team)
//                .where(team.name.eq("teamA"))
//                .fetch();

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .join(member.team, team)
                //.on(team.name.eq("teamA"))
                //이렇게 하면 .on했을 때랑 결과가 같다.
                //inner join일 떈 on절로 걸라내나 where로 걸러내나 같다.
                //반면에 left나 right join를 할 때는 on를 쓴다.
                .where(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 연관관계 없는 엔티티를 외부 조인(세타조인)
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    public void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        /*
        - from 절에 여러 엔티티를 선택해서 세타 조인
        - 외부 조인 불가능 다음에 설명할 조인 on을 사용하면 외부 조인 가능
        */
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);

        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    /**
     * 페치 조인은 SQL에서 제공하는 기능은 아니다. SQL조인을 활용해서 연관된 엔티티를 SQL 한번에
     * 조회하는 기능이다. 주로 성능 최적화에 사용하는 방법이다.
     */
    //페치조인 적용 안 한 코드
    @Test
    public void fetchJoinNo() {
        //영속성 컨텍스트를 db로 날리고 시작하자
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        //findMember.getTeam() 로딩이 된 엔티티인지 초기화가 안된 엔티티인지 알려주는 것이다.
        //지금 fetchJoin를 적용을 안 했으니 false가 나와야 하는 것이다.
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    //fetchJoin 적용
    @Test
    public void fetchJoinUse() {
        //영속성 컨텍스트를 db로 날리고 시작하자
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                //fetchJoin()이 들어감
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery() {

        QMember memberSub = new QMember("memberSub");//alias가 중복되면 안되는 경우 이렇게 만들어줘야 함


        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(40);

    }

    /**
     * 나이가 평균 이상인 회원
     */
    @Test
    //greater or equal
    public void subQueryGoe() {

        QMember memberSub = new QMember("memberSub");//alias가 중복되면 안되는 경우 이렇게 만들어줘야 함


        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(30, 40);

    }

    /**
     * 나이가 평균 이상인 회원
     */
    //약간 억지성 코드이고 전혀 효율적이지 않음 그냥 in절을 보여주는 예시
    @Test
    public void subQueryIn() {

        QMember memberSub = new QMember("memberSub");//alias가 중복되면 안되는 경우 이렇게 만들어줘야 함


        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);

    }

    /**
     * from 절의 서브쿼리 한계
     * JPA JPQL 서브쿼리의 한계점으로 from 절의 서브쿼리(인라인 뷰)는 지원하지 않는다. 당연히 Querydsl
     * 도 지원하지 않는다. 하이버네이트 구현체를 사용하면 select 절의 서브쿼리는 지원한다. Querydsl도
     * 하이버네이트 구현체를 사용하면 select 절의 서브쿼리를 지원한다
     */
    /**
     * from 절의 서브쿼리 해결방안
     * 1. 서브쿼리를 join으로 변경한다. (가능한 상황도 있고, 불가능한 상황도 있다.)
     * 2. 애플리케이션에서 쿼리를 2번 분리해서 실행한다. (상황에 따라 다르다 성능을 보면서 해야 한다)
     * 3. nativeSQL을 사용한다.
     */
    @Test
    public void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");//alias가 중복되면 안되는 경우 이렇게 만들어줘야 함

        List<Tuple> result = queryFactory
                .select(member.username,
                        //JPAExpression를 static import으로 바꿔버림
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void basicCase() {
        List<String> result = queryFactory.select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }


    }

    @Test
    public void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void constant(){
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }

    @Test
    public void concat(){

        //username_age 이렇게 만들고 싶다. 근데 주의 username은 문자고 age는 숫자다
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void simpleProjection(){
        List<String> fetch1 = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : fetch1) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void tupleProjection(){
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);

            System.out.println("username = "+ username);
            System.out.println("age = " + age);
        }
    }

    @Test
    public void findDtoByJPQL(){
        //언제 패키지 명 다 적고 있나... 정말 별로다.
        /**
         * 순수 JPA에서 DTO를 조회할 때는 new 명령어를 사용해야함
         * DTO의 package이름을 다 적어줘야해서 지저분함
         * 생성자 방식만 지원함
         */
        List<MemberDto> resultList = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * DTO 반환할 때 사용
     * 다음 3가지 방법 지원
     * 프로퍼티 접근
     * 필드 직접 접근
     * 생성자 사용
     */

    @Test
    public void findDtoBySetter(){
        List<MemberDto> result = queryFactory
                //bean 방식은 getter setter를 통해 들어가는 방식이다.
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByField(){
        List<MemberDto> result = queryFactory
                //field 방식은 getter setter 무시하고 바로 필드에 꽂힌다.
                //private인데 어떻게 넣어주나 싶은데 그런 걸 해주는 라이브러리들이 있다.
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }
    @Test
    public void findDtoByConstructor(){
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findUserDto(){
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        /**
                         * 프로퍼티나, 필드 접근 생성 방식에서 이름이 다를 때 해결 방안
                         * ExpressionUtils.as(source,alias) : 필드나, 서브 쿼리에 별칭 적용
                         * username.as("memberName") : 필드에 별칭 적용
                         */
                        member.username.as("name"),
                        //서브쿼리는 ExpresionUtils로 감쌀 수밖에 없다.
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    public void findUserDtoByConstructor(){
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    public void findDtoByQueryProjection(){
        List<MemberDto> result = queryFactory
                /**
                 * 이게 constructor 방식보다 좋은 이유는 에러를 컴파일 타임에서 확인할 수 있다.
                 * 반면에 constructor 방식은 runtime 에러에서 확인 가능하다.
                 * 예를 들어 constructor 방식에서
                 * .select(Projections.constructor(MemberDto.class,
                 *                         member.username,
                 *                         member.age,
                 *                         member.id))...
                 *  member.id가 들어가면 이건 컴파일 타임에서 확인이 안된다. 런타임에서 에러가 발생한다.
                 *  반면에 아래와 같이 하면 컴파일 타임에서 에러를 확인 가능하다.
                 */
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void dynamicQuery_BooleanBuilder(){
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if(usernameCond != null){
            builder.and(member.username.eq(usernameCond));
        }

        if(ageCond != null){
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    /**
     * 이 방식이 너무 좋다 보통 이 방법을 몰라서 BooleanBuilder를 많이 사용하지만 강사님을 이 방식을 좋아하신다.
     */
    @Test
    public void dynamicQuery_WhereParam(){
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    /**
     * BooleanBuilder보다 엄청난 장점은 메서드를 만들어서 조립이 가능하다는 것이다.
     * 그리고 가독성이 훨씬 좋다. (이 방식을 더 선호하신다)
     */
    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                .fetch();
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }


    private BooleanExpression allEq(String usernameCond, Integer ageCond){
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    @Test
    /**
     * bulk 연산은 조심해야 할 부분이 있다.
     * JPA는 영속성 컨텍스트라는 것에
     *엔티티가 다 올라가 있다. 지금 member1~member4가 영속성 컨텍스트에 올라가 있다.
     * 아래 쿼리를 실행하면 이 bulk 연산은 영속성 컨텍스트를 무시하고 바로 db에 적용된다.
     * 즉, db 상태와 영속성 상태가 달라지게 되는 것이다.
     *
     * > 주의: JPQL 배치와 마찬가지로, 영속성 컨텍스트에 있는 엔티티를 무시하고 실행되기 때문에 배치 쿼리를
     * 실행하고 나면 영속성 컨텍스트를 초기화 하는 것이 안전하다.
     *
     * 그리고 select할 때 기본적으로 영속성 컨텍스트가 우선권을 가짐으로 db와 영속성 컨텍스트 내용이 다를 땐
     * 영속성 컨텍스트의 내용을 사용하게 되는 문제가 발생한다. */
    public void bulkUpdate(){
        //member1 = 10 -> 비회원
        //member2 = 20 -> 비회원
        //member3 = 30 -> 유지
        //member4 = 40 -> 유지
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        em.flush();
        em.clear();

        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
            /**
             * 결과
             * member1 = Member(id=3, username=member1, age=10)
             * member1 = Member(id=4, username=member2, age=20)
             * member1 = Member(id=5, username=member3, age=30)
             * member1 = Member(id=6, username=member4, age=40)
             *
             * 즉 db에 처음에 update된 것이 반영 안됨!!!! (영속성 컨텍스트가 반영이 되기 때문) 그래서 em.flush()와 em.clear()를 해주는 것
             */
        }

    }

    @Test
    public void bulkAdd(){
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.multiply(2)) //빼기 할 떄는 minus가 없고 그냥 add(-1) 이렇게 하면 된다.
                .execute();
    }

    @Test
    public void bulkDelete(){
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    /**
     * SQL 함수 사용하는 방법
     */
    @Test
    public void sqlFunction(){
        //여기선 member들의 username에서 member를 M으로 대체하겠다는 것이다.
        List<String> result = queryFactory
                .select(
                        Expressions.stringTemplate("function('replace', {0}, {1}, {2})",
                                member.username, "member", "M"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void sqlFunction2(){
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
/*                .where(member.username.eq(
                        Expressions.stringTemplate("function('lower', {0})", member.username)))*/
                //ANSI 표준에서 제공하는 건 웬만해서는 이렇게 체인으로 다 있다.
                .where(member.username.eq(member.username.lower()))
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

}


















