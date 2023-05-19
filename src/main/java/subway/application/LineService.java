package subway.application;

import static subway.application.StationFactory.toStation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import subway.dao.LineDao;
import subway.dao.LineEntity;
import subway.dao.SectionDao;
import subway.dao.SectionEntity;
import subway.dao.StationDao;
import subway.domain.Distance;
import subway.domain.Section;
import subway.domain.Sections;
import subway.domain.Station;
import subway.dto.LineSaveDto;
import subway.dto.LineStationResponse;
import subway.dto.StationResponse;

@Transactional(readOnly = true)
@Service
public class LineService {
    private final LineDao lineDao;
    private final StationDao stationDao;
    private final SectionDao sectionDao;

    public LineService(LineDao lineDao, StationDao stationDao, SectionDao sectionDao) {
        this.lineDao = lineDao;
        this.stationDao = stationDao;
        this.sectionDao = sectionDao;
    }

    @Transactional
    public LineStationResponse saveLine(LineSaveDto request) {
        Long savedId = lineDao.insert(new LineEntity(request.getLineName()));

        return new LineStationResponse(savedId, request.getLineName(), null);
    }

    public List<LineStationResponse> findLineResponses() {
        List<LineEntity> persistLines = lineDao.findAll();
        return persistLines.stream()
                .map(entities -> {
                    Long lineId = entities.getId();
                    return getLineResponse(entities, lineId);
                })
                .collect(Collectors.toList());
    }

    private LineStationResponse getLineResponse(LineEntity entities, Long lineId) {
        List<SectionEntity> findSections = sectionDao.findByLineId(lineId);
        List<Section> collect = findSections.stream()
                .map(this::toSection)
                .collect(Collectors.toList());

        if (collect.isEmpty()) {
            return new LineStationResponse(entities.getId(), entities.getName(), new ArrayList<StationResponse>());
        }

        Sections sections = new Sections(collect);

        List<Station> sortedStations = sections.getSortedStations();

        List<StationResponse> stationsResponses = sortedStations.stream()
                .map(it -> {
                    Long findStationId = stationDao.findIdByName(it.getName());
                    return new StationResponse(findStationId, it.getName());
                })
                .collect(Collectors.toList());

        return new LineStationResponse(entities.getId(), entities.getName(), stationsResponses);
    }

    private Section toSection(SectionEntity sectionEntity) {
        Station startStation = toStation(stationDao.findById(sectionEntity.getStartStationId()));
        Station endStation = toStation(stationDao.findById(sectionEntity.getEndStationId()));
        Distance distance = new Distance(sectionEntity.getDistance());

        return new Section(startStation, endStation, distance);
    }

    public LineStationResponse findLineResponseById(Long id) {
        LineEntity findEntity = lineDao.findById(id);
        return getLineResponse(findEntity, id);
    }

}
