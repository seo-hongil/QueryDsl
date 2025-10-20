package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.domain.Hello;
import study.querydsl.domain.QHello;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest

class QueryDslApplicationTests {

    @PersistenceContext
    EntityManager em;

    @Test
    @Transactional
    void contextLoads() {
        Hello hello = new Hello();

        em.persist(hello);

        JPAQueryFactory query = new JPAQueryFactory(em);
        QHello qHello = QHello.hello;

        Hello result = query
                .selectFrom(qHello)
                .fetchOne();

        assertThat(result).isEqualTo(hello);
        assertThat(result.getId()).isEqualTo(hello.getId());
    }

}
