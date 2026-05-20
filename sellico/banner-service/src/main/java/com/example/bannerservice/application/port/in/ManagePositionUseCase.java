package com.example.bannerservice.application.port.in;

import com.example.bannerservice.application.command.CreatePositionCommand;
import com.example.bannerservice.application.command.UpdatePositionCommand;
import com.example.bannerservice.domain.model.BannerPosition;

import java.util.List;

public interface ManagePositionUseCase {
    BannerPosition createPosition(CreatePositionCommand command);
    BannerPosition updatePosition(UpdatePositionCommand command);
    void deletePosition(String code);
    List<BannerPosition> getAllPositions();
    List<BannerPosition> getActivePositions();
    BannerPosition getPositionByCode(String code);
}

