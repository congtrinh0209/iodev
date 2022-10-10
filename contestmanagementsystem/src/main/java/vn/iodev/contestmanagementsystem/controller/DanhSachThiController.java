package vn.iodev.contestmanagementsystem.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
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

import vn.iodev.contestmanagementsystem.exception.ResourceNotFoundException;
import vn.iodev.contestmanagementsystem.helper.ExcelHelper;
import vn.iodev.contestmanagementsystem.model.CuocThi;
import vn.iodev.contestmanagementsystem.model.DanhSachThi;
import vn.iodev.contestmanagementsystem.model.DoanThi;
import vn.iodev.contestmanagementsystem.model.ImportResponse;
import vn.iodev.contestmanagementsystem.model.KhoiThi;
import vn.iodev.contestmanagementsystem.payload.ToChucResponse;
import vn.iodev.contestmanagementsystem.repository.CuocThiRepository;
import vn.iodev.contestmanagementsystem.repository.DanhSachThiRepository;
import vn.iodev.contestmanagementsystem.service.ExcelService;

@RestController
@RequestMapping("/api")
public class DanhSachThiController {
    @Autowired
    DanhSachThiRepository danhSachThiRepository;

    @Autowired
    CuocThiRepository cuocThiRepository;

    @Autowired
    ExcelService fileService;

    @GetMapping("/danhsachthis")
    public List<DanhSachThi> getAllDanhSachThis(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "15") int size, @RequestParam(required = false) String cuocThiId, @RequestParam(required = false) String khoiThiId, @RequestParam(required = false) String doiThiId) {
        Pageable paging = PageRequest.of(page - 1, size);
        Optional<CuocThi> cuocThiData = cuocThiRepository.findById(cuocThiId);
        if (cuocThiData.isPresent()) {
            return danhSachThiRepository.findDanhSachThiByMultipleConditions(cuocThiData.get(), khoiThiId, doiThiId, paging);
        }
        else {
            return danhSachThiRepository.findDanhSachThiByMultipleConditions(null, khoiThiId, doiThiId, paging);
        }
    }

    @GetMapping("/cuocthis/{cuocThiId}/danhsachthis")
    public ResponseEntity<List<DanhSachThi>> getAllDanhSachThisByCuocThiId(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "15") int size, @PathVariable(value = "cuocThiId") String cuocThiId) {
        List<DanhSachThi> lstDanhSachThi = new ArrayList<>();

        if (!cuocThiRepository.existsById(cuocThiId)) {
            return new ResponseEntity<>(lstDanhSachThi, HttpStatus.OK);
        }

        Pageable paging = PageRequest.of(page - 1, size);

        List<DanhSachThi> danhSachThis = danhSachThiRepository.findByCuocThiId(cuocThiId, paging);
        lstDanhSachThi.addAll(danhSachThis);

        return new ResponseEntity<>(lstDanhSachThi, HttpStatus.OK);
    }

    @GetMapping("/danhsachthis/{danhSachId}")
    public ResponseEntity<DanhSachThi> getDanhSachThiById(@PathVariable(value = "danhSachId", required = true) long danhSachId)
        throws ResourceNotFoundException {
        DanhSachThi danhSachThi = danhSachThiRepository.findById(danhSachId)
          .orElseThrow(() -> new ResourceNotFoundException("DanhSachThi not found for this id::" + danhSachId));
        return ResponseEntity.ok().body(danhSachThi);
    }

    @PostMapping("/cuocthis/{cuocThiId}/danhsachthis")
    public ResponseEntity<DanhSachThi> createDanhSachThiOfCuocThi(@PathVariable(value = "cuocThiId") String cuocThiId, @Valid @RequestBody DanhSachThi danhSachThi, @RequestHeader("vaiTros") String vaiTros) {
        try {
            DanhSachThi danhSachThiMoi = cuocThiRepository.findById(cuocThiId).map(cuocThi -> {
                danhSachThi.setCuocThi(cuocThi);
                return danhSachThiRepository.save(danhSachThi);
            }).orElseThrow(() -> new ResourceNotFoundException("Not found CuocThi with id = " + cuocThiId));

            return new ResponseEntity<>(danhSachThiMoi, HttpStatus.CREATED);
        } catch (ResourceNotFoundException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }   
    }

    @PostMapping("/danhsachthis")
    public ResponseEntity<DanhSachThi> createDanhSachThi(@RequestBody DanhSachThi danhSachThi) {
        try {
            DanhSachThi _danhSachThi = danhSachThiRepository.save(new DanhSachThi(
                danhSachThi.getThiSinhId(),
                danhSachThi.getCuocThi(),
                danhSachThi.getKhoiThiId(),
                danhSachThi.getDoiThiId(),
                danhSachThi.getChuDeSangTao(),
                danhSachThi.getSoBaoDanh(),
                danhSachThi.getKetQuaSoLoai(),
                danhSachThi.getThuTuXepHang(),
                danhSachThi.getBangDiemThi(),
                danhSachThi.getHangGiaiThuong()    
            ));
            return new ResponseEntity<>(_danhSachThi, HttpStatus.CREATED);
        }
        catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/danhsachthis/{id}")
    public ResponseEntity<DanhSachThi> updateDanhSachThi(@PathVariable("id") long id, @RequestBody DanhSachThi danhSachThi) {
        Optional<DanhSachThi> danhSachThiData = danhSachThiRepository.findById(id);
        if (danhSachThiData.isPresent()) {
            DanhSachThi _danhSachThi = danhSachThiData.get();
            if (danhSachThi.getThiSinhId() != null) {
                _danhSachThi.setThiSinhId(danhSachThi.getThiSinhId());
            }
            if (danhSachThi.getKhoiThiId() != null) {
                _danhSachThi.setKhoiThiId(danhSachThi.getKhoiThiId());
            }
            if (danhSachThi.getDoiThiId() != null) {
                _danhSachThi.setDoiThiId(danhSachThi.getDoiThiId());
            }
            if (danhSachThi.getChuDeSangTao() != null) {
                _danhSachThi.setChuDeSangTao(danhSachThi.getChuDeSangTao());
            }
            if (danhSachThi.getSoBaoDanh() != null) {
                _danhSachThi.setSoBaoDanh(danhSachThi.getSoBaoDanh());
            }
            if (danhSachThi.getKetQuaSoLoai() != null) {
                _danhSachThi.setKetQuaSoLoai(danhSachThi.getKetQuaSoLoai());
            }
            if (danhSachThi.getThuTuXepHang() != null) {
                _danhSachThi.setThuTuXepHang(danhSachThi.getThuTuXepHang());
            }
            if (danhSachThi.getBangDiemThi() != null) {
                _danhSachThi.setBangDiemThi(danhSachThi.getBangDiemThi());
            }
            if (danhSachThi.getHangGiaiThuong() != null) {
                _danhSachThi.setHangGiaiThuong(danhSachThi.getHangGiaiThuong());
            }
            _danhSachThi.setThoiGianCapNhat(System.currentTimeMillis());
            
            return new ResponseEntity<>(danhSachThiRepository.save(_danhSachThi), HttpStatus.OK);
        }
        else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/danhsachthis/{id}")
    public ResponseEntity<HttpStatus> deleteDanhSachThi(@PathVariable("id") long id) {
        try {
            danhSachThiRepository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/cuocthis/{cuocThiId}/danhsachthis")
    public ResponseEntity<List<DanhSachThi>> deleteAllDanhSachThisOfCuocThi(@PathVariable(value = "cuocThiId") String cuocThiId, @RequestHeader("vaiTros") String vaiTros) {
        if (!cuocThiRepository.existsById(cuocThiId)) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        danhSachThiRepository.deleteByCuocThiId(cuocThiId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/danhsachthis/import")
    public ResponseEntity<ImportResponse> importDanhSachThi(@RequestParam("file") MultipartFile multipartFile, @RequestParam("fileType") String fileType) {
        String fileName = StringUtils.cleanPath(multipartFile.getOriginalFilename());
        long size = multipartFile.getSize();
        String message = "";

        if (ExcelHelper.hasExcelFormat(multipartFile)) {
            List<ToChucResponse> tochucs = fileService.importToChuc(multipartFile);

            List<CuocThi> cuocthis = fileService.importCuocThi(multipartFile);
            
            List<KhoiThi> khoithis = fileService.importKhoiThi(multipartFile, cuocthis);
            List<DoanThi> doanthis = new ArrayList<>();

            for (CuocThi ct : cuocthis) {
                fileService.importHuanLuyenVien(multipartFile, tochucs, khoithis, doanthis, ct);
            }

            fileService.importDanhSachThi(multipartFile, cuocthis, khoithis, doanthis);

            message = "Import DanhSachThi successfully: " + multipartFile.getOriginalFilename();
            return ResponseEntity.status(HttpStatus.OK).body(new ImportResponse(fileName, size, message));
        }

        message = "Please import an excel file!";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ImportResponse(fileName, size, message));
    }
}