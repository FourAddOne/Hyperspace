package com.lihuahua.hyperspace.Service;

import com.lihuahua.hyperspace.models.dto.CreateGroupDTO;
import com.lihuahua.hyperspace.models.vo.GroupVO;

public interface GroupService {

    GroupVO createGroup(CreateGroupDTO createGroupDTO);
}
