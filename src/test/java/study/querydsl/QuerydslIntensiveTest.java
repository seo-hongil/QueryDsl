package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.domain.dto.MemberDto;
import study.querydsl.domain.dto.QMemberDto;
import study.querydsl.domain.dto.UserDto;
import study.querydsl.domain.Member;
import study.querydsl.domain.QMember;
import study.querydsl.domain.Team;


import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.domain.QMember.*;
import static com.querydsl.jpa.JPAExpressions.*;

@SpringBootTest
@Transactional
public class QuerydslIntensiveTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before(){
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("temaA");
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

    // 프로젝션 대상이 하나일 경우
    // 태상이 하나이면 타입이 명확히 지정 (둘 이상이면 튜플 or DTO로 조회)

    // 하나
    @Test
    public void simpleProjection() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    // 둘 이상
    @Test
    public void tupleProjection() {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    // 조회결과를 DTO로 반환

    // 순수 JPA로 DTO 조회
    @Test
    public void findDtoByJPQL() {
        List<MemberDto> result = em.createQuery("select new study.querydsl.domain.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    // 프로퍼티 접근 방식 (엔티티의 필드와, DTO의 필드명이 일치 해야함)
    @Test
    public void findDtoBySetter() {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        // Projections.bean: DTO의 Setter로 접근해서 DTO를 생성 ( DTO 기본 생성자 필수 )

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    // 필드 직접 접근 방식(엔티티의 필드와, DTO의 필드명이 일치 해야함)
    @Test
    public void findDtoByField() {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        // Projections.fields: DTO의 필드에 직접 값을 대입

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    // 필드명이 다를 경우(별칭사용)
    @Test
    public void findUserDto() {
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"), //필드에 별칭 적용
                        ExpressionUtils.as(
                                select(memberSub.age.max())
                                        .from(memberSub), "age")
                ))
                .from(member)
                .fetch();
                // ExpressionUtils.as(source,alias): 필드 or 서브 쿼리에 별칭을 적용

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    // 생성자 사용
    @Test
    public void findDtoByConstructor() {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        // Projections.constructor: 생성자 사용 방식의 경우 엔티티와 DTO의 별칭이 달라도 타입이 맞는 생성자가 있다면 DTO 생성이 가능

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    // 프로젝션 결과 반환(@QueryProjection)
    // 해당 DTO의 생성자에 @QueryProjection 붙여야 함.
    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    // Projections.constructor는 생성자 인자 값의 타입이나 개수가 다를 때, 런타임 오류로 잡을 수 있다.
    // @QueryProjection은 생성자 인자 값이 타입이나 개수가 다를 때, 컴파일 오류로 바로 잡을 수 있다.

    // 단점
    // 1. DTO에 Querydsl 애노테이션을 유지하고, DTO까지 Q 파일을 생성해야 한다.
    //2. DTO가 Querydsl에 의존하게 된다.
        //-> Querydsl을 더 이상 사용하지 않는다면 DTO 역시 수정해야 한다.
        //-> DTO는 리포지토리가 아닌 다양한 레이어에서 사용할 수 있는데, Querydsl에 의존하는 DTO는 순수하지 않다.


    // 동적 쿼리

    // 1. BooleanBuilder
    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    // 2. where절 다중 파라미터 사용
    @Test
    public void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    // where 조건에 null 값은 무시된다.
    // 메서드의 반환 타입은 Predicate와 BooleanExpression 모두 가능

    // 수정, 삭제 배치 쿼리

        // 쿼리 한번으로 대량 데이터 수정
    @Test
    public void bulkUpdate() {

        //member1 = 10 -> 비회원
        //member2 = 20 -> 비회원
        //member3 = 30 -> 유지
        //member4 = 40 -> 유지

        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();
    }

    // 여기서 벌크 연산은 DB에 직접 SQL을 실행하기 때문에,영속성 컨텍스트에 이미 로드된 엔티티가 있다면 DB와 데이터가 달라질 수 있다.
    // 즉, 먼저 조회된 1차 캐시에 데이터가 있다면 수정된 데이터로 조회 x, (단, 영속성 컨텍스트에 없으면 그런 문제 없음!)
    // 그래서  벌크 연산 이후에 영속성 컨텍스트를 초기화 해주기!
    @Test
    public void bulkUpdate2() {

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
    }

    // 대량 삭제
    @Test
    public void bulkDelete() {
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    // SQL function 호출
    // SQL function은 현재 사용하는 DB의 Dialect에 등록된 내용만 호출 가능

    // member -> M으로 변경하는 replace 함수 사용
    @Test
    public void sqlFunction() {
        List<String> result = queryFactory
                .select(
                        Expressions.stringTemplate(
                                "function('replace', {0}, {1}, {2})",
                                member.username, "member", "M"))
                .from(member)
                .fetch();
        // lower 같은 ansi 표준 함수들은 querydsl이 상당부분 내장하고 있다.
        // -> .where(member.username.eq(member.username.lower())) 로 처리 가능!

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    //소문자로 변경해서 비교 - lower 함수 사용
    @Test
    public void sqlFunction2() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(
                        Expressions.stringTemplate("function('lower', {0})", member.username)))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }


}
