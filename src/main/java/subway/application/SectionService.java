package subway.application;

import static subway.application.StationFactory.toStation;
import static subway.domain.ChangeSectionStatus.FOR_MIDDLE_SECTION;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import subway.dao.SectionDao;
import subway.dao.SectionEntity;
import subway.dao.StationDao;
import subway.dao.StationEntity;
import subway.domain.ChangeSections;
import subway.domain.Distance;
import subway.domain.Section;
import subway.domain.Sections;
import subway.domain.Station;
import subway.dto.SectionSaveDto;

@Transactional(readOnly = true)
@Service
public class SectionService {

    private final SectionDao sectionDao;
    private final StationDao stationDao;
    private final SectionsMapper sectionsMapper;

    public SectionService(SectionDao sectionDao, StationDao stationDao, final SectionsMapper sectionsMapper) {
        this.sectionDao = sectionDao;
        this.stationDao = stationDao;
        this.sectionsMapper = sectionsMapper;
    }

    @Transactional
    public void saveSection(Long lineId, SectionSaveDto request) {
        List<SectionEntity> sectionEntities = sectionDao.findByLineId(lineId);
        Sections sections = sectionsMapper.mapFrom(sectionEntities);

        Section newSection = getNewSection(request);

        ChangeSections sectionsForUpdate = sections.add(newSection);

        saveNewStationIfNotExists(newSection.getStartStation());
        saveNewStationIfNotExists(newSection.getEndStation());

        updateSections(lineId, sectionsForUpdate);
    }

    private void updateSections(final Long lineId, final ChangeSections sectionsForUpdate) {
        updateSectionIfChangedMiddleSection(lineId, sectionsForUpdate);
        SectionEntity newSectionEntity = makeSectionEntity(lineId, sectionsForUpdate.getInsertOrRemoveSection());
        sectionDao.insert(newSectionEntity);
    }

    private Section getNewSection(final SectionSaveDto request) {
        Station requestStartStation = new Station(request.getStartStation());
        Station requestEndStation = new Station(request.getEndStation());
        Distance requestDistance = new Distance(request.getDistance());

        return Section.builder()
                .startStation(requestStartStation)
                .endStation(requestEndStation)
                .distance(requestDistance)
                .build();
    }

    private void saveNewStationIfNotExists(Station station) {
        if (!stationDao.isExistStationByName(station.getName())) {
            stationDao.insert(new StationEntity(station.getName()));
        }
    }

    private SectionEntity makeSectionEntity(Long lineId, Section section) {
        Station startStation = section.getStartStation();
        Station endStation = section.getEndStation();

        Long startStationId = stationDao.findIdByName(startStation.getName());
        Long endStationId = stationDao.findIdByName(endStation.getName());

        return new SectionEntity(lineId, startStationId, endStationId, section.getDistance().getDistance());
    }

    @Transactional
    public void deleteSection(Long lineId, Long stationId) {
        List<SectionEntity> sectionEntitiesOfLine = sectionDao.findByLineId(lineId);
        Sections sections = sectionsMapper.mapFrom(sectionEntitiesOfLine);

        Station removedStation = toStation(stationDao.findById(stationId));
        ChangeSections sectionsForRemove = sections.remove(removedStation);

        removeSections(lineId, sectionsForRemove);
    }

    private void removeSections(final Long lineId, final ChangeSections sectionsForRemove) {
        updateSectionIfChangedMiddleSection(lineId, sectionsForRemove);
        SectionEntity removeSectionEntity = makeSectionEntity(lineId, sectionsForRemove.getInsertOrRemoveSection());
        sectionDao.delete(removeSectionEntity);
    }

    private void updateSectionIfChangedMiddleSection(final Long lineId, final ChangeSections sectionsForRemove) {
        if (sectionsForRemove.is(FOR_MIDDLE_SECTION)) {
            SectionEntity updateSectionEntity = makeSectionEntity(lineId, sectionsForRemove.getUpdateSection());
            sectionDao.update(updateSectionEntity);
        }
    }

}
