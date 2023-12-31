package team.a501.rif.repository.achievement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import team.a501.rif.domain.achievement.Achievement;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
class AchievementRepositoryTest {

    @Autowired
    private AchievementRepository achievementRepository;

    @BeforeEach
    void setUp(){
        // 업적 추가
        Achievement achievement = Achievement.builder()
                .tier(0)
                .title("업적 1")
                .description("Lorem ipsum dolor sit amet")
                .imgPath("/achievement/1.png")
                .build();

        achievementRepository.save(achievement);
    }

    @AfterEach
    void cleanUp(){
        achievementRepository.deleteAll();
    }

    @DisplayName("업적 추가")
    @Test
    void createAchievement(){

        Achievement achievement = Achievement.builder()
                .tier(0)
                .title("업적 2")
                .description("consectetur adipiscing elit")
                .imgPath("/achievement/2.png")
                .build();

        achievementRepository.save(achievement);

        long count = achievementRepository.count();
        assertThat(count).isEqualTo(2L);
    }

    @DisplayName("전체 업적 조회")
    @Test
    void findAllAchievement(){

        List<Achievement> all = achievementRepository.findAll();
        for(var achievement: all){
            System.out.println(achievement);
        }

        assertThat(all.size()).isEqualTo(1L);
    }
    
}