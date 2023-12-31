package team.a501.rif.service.badge;

import team.a501.rif.domain.badge.BadgeAcq;
import team.a501.rif.dto.badge.BadgeAcqInfo;

public interface BadgeAcqService {

    BadgeAcqInfo save(String memberId, Long badgeId);

    void delete(BadgeAcq badgeAcq);
}
