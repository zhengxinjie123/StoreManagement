package com.joao.storemanagement.mapper.primary;

import com.joao.storemanagement.dto.system.RecentApiErrorDTO;
import com.joao.storemanagement.dto.system.RecentImportFailureDTO;
import com.joao.storemanagement.dto.system.RecentSyncIssueDTO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemHealthMapper {

    List<RecentApiErrorDTO> selectRecentApiErrors();

    List<RecentImportFailureDTO> selectRecentImportFailures();

    List<RecentSyncIssueDTO> selectRecentSyncIssues();
}
