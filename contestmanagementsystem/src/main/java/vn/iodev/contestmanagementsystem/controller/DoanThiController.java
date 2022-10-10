package vn.iodev.contestmanagementsystem.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import vn.iodev.contestmanagementsystem.exception.ResourceNotFoundException;
import vn.iodev.contestmanagementsystem.helper.ExcelHelper;
import vn.iodev.contestmanagementsystem.model.DoanThi;
import vn.iodev.contestmanagementsystem.model.ImportResponse;
import vn.iodev.contestmanagementsystem.model.LoaiTaiKhoan;
import vn.iodev.contestmanagementsystem.repository.DoanThiRepository;
import vn.iodev.contestmanagementsystem.security.VaiTroChecker;
import vn.iodev.contestmanagementsystem.service.ExcelService;

@RestController
@RequestMapping("/api")
@Slf4j
public class DoanThiController {
    @Autowired
    DoanThiRepository doanThiRepository;

    @Autowired
    ExcelService fileService;

    @GetMapping("/doanthis")
    public List<DoanThi> getAllDoanThis(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "15") int size, @RequestParam(required = false) String toChucId, @RequestParam(required = false) String cuocThiId, @RequestHeader("id") String taiKhoanId, @RequestHeader("loaiTaiKhoan") Integer loaiTaiKhoan, @RequestHeader("vaiTros") String vaiTros) {
        List<DoanThi> lstDoanThi = new ArrayList<>();
        log.info("VaiTro: " + vaiTros + ", " + VaiTroChecker.hasVaiTroQuanTriToChuc(vaiTros) + ", " + VaiTroChecker.hasVaiTroQuanTriHeThong(vaiTros));
        if (!VaiTroChecker.hasVaiTroQuanTriToChuc(vaiTros) && !VaiTroChecker.hasVaiTroQuanTriHeThong(vaiTros)) {
            return lstDoanThi;
        }
        if (VaiTroChecker.hasVaiTroQuanTriHeThong(vaiTros)) {
            Pageable paging = PageRequest.of(page - 1, size);
            log.info("To chuc id: " + toChucId + ", cuoc thi id: " + cuocThiId);
            if (toChucId == null && cuocThiId != null) {
                return doanThiRepository.findByCuocThiId(cuocThiId, paging);
            }
            else if (toChucId != null && cuocThiId == null) {
                return doanThiRepository.findByToChucId(toChucId, paging);
            }
            else if (toChucId != null && cuocThiId != null) {
                return doanThiRepository.findByToChucIdAndCuocThiId(toChucId, cuocThiId, paging);
            }
            else {
                Page<DoanThi> doanThis;
                doanThis = doanThiRepository.findAll(paging);
                return doanThis.getContent();
            }
        }
        else if (VaiTroChecker.hasVaiTroQuanTriToChuc(vaiTros)) {
            if (loaiTaiKhoan == LoaiTaiKhoan.TAIKHOAN_TOCHUC && taiKhoanId !=null && !taiKhoanId.isEmpty()) {
                Pageable paging = PageRequest.of(page - 1, size);
                lstDoanThi.addAll(doanThiRepository.findByToChucId(taiKhoanId, paging));
                return lstDoanThi;
            }
            else {
                return lstDoanThi;
            }
        }
        else {
            return lstDoanThi;
        }
    }

    @GetMapping("/doanthis/{id}")
    public ResponseEntity<DoanThi> getDoanThiById(@PathVariable(value = "id") String doanThiId)
        throws ResourceNotFoundException {
        DoanThi doanThi = doanThiRepository.findById(doanThiId)
          .orElseThrow(() -> new ResourceNotFoundException("DoanThi not found for this id :: " + doanThiId));
        return ResponseEntity.ok().body(doanThi);
    }

    @PostMapping("/doanthis")
    public ResponseEntity<DoanThi> createDoanThi(@RequestBody DoanThi doanThi) {
        try {
            DoanThi _doanThi = doanThiRepository.save(new DoanThi(doanThi.getTenGoi(), doanThi.getTiengAnh(), doanThi.getDiaChiHoatDong(), doanThi.getEmail(), doanThi.getToChucId(), doanThi.getCuocThiId()));
            return new ResponseEntity<>(_doanThi, HttpStatus.CREATED);
        }
        catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/doanthis/import")
    public ResponseEntity<ImportResponse> importDoanThi(@RequestParam("file") MultipartFile multipartFile, @RequestParam("fileType") String fileType) {
        String fileName = StringUtils.cleanPath(multipartFile.getOriginalFilename());
        long size = multipartFile.getSize();
        String message = "";
        if (ExcelHelper.hasExcelFormat(multipartFile)) {
            try {
                fileService.importDoanThi(multipartFile);
                message = "Import DoanThi successfully: " + multipartFile.getOriginalFilename();
                return ResponseEntity.status(HttpStatus.OK).body(new ImportResponse(fileName, size, message));
            }
            catch (Exception e) {
                e.printStackTrace();
                message = "Cound not import DoanThi: " + multipartFile.getOriginalFilename();
                return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ImportResponse(fileName, size, message));
            }
        }

        message = "Please import an excel file!";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ImportResponse(fileName, size, message));
    }

    @PutMapping("/doanthis/{id}")
    public ResponseEntity<DoanThi> updateDoanThi(@PathVariable("id") String id, @RequestBody DoanThi doanThi) {
        Optional<DoanThi> doanThiData = doanThiRepository.findById(id);
        if (doanThiData.isPresent()) {
            DoanThi _doanThi = doanThiData.get();
            if (doanThi.getTenGoi() != null) {
                _doanThi.setTenGoi(doanThi.getTenGoi());
            }
            if (doanThi.getTiengAnh() != null) {
                _doanThi.setTiengAnh(doanThi.getTiengAnh());
            }
            if (doanThi.getDiaChiHoatDong() != null) {
                _doanThi.setDiaChiHoatDong(doanThi.getDiaChiHoatDong());;
            }
            if (doanThi.getEmail() != null) {
                _doanThi.setEmail(doanThi.getEmail());;
            }
            if (doanThi.getToChucId() != null) {
                _doanThi.setToChucId(doanThi.getToChucId());
            }
            if (doanThi.getCuocThiId() != null) {
                _doanThi.setCuocThiId(doanThi.getCuocThiId());
            }
            if (doanThi.getThuTuXepHang() != null) {
                _doanThi.setThuTuXepHang(doanThi.getThuTuXepHang());
            }
            _doanThi.setThoiGianCapNhat(System.currentTimeMillis());
            
            return new ResponseEntity<>(doanThiRepository.save(_doanThi), HttpStatus.OK);
        }
        else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/doanthis/{id}")
    public ResponseEntity<HttpStatus> deleteDoanThi(@PathVariable("id") String id) {
        try {
            doanThiRepository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}