package com.joao.storemanagement.service.system;

import com.joao.storemanagement.vo.system.DatabaseResetResultVO;

public interface DatabaseResetService {

    DatabaseResetResultVO resetAllData(String confirmPassword);
}
