package com.neverpile.eureka.rest.configuration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.annotation.PostConstruct;

import org.modelmapper.AbstractConverter;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import com.neverpile.eureka.impl.documentservice.DocumentPdo;
import com.neverpile.eureka.model.ContentElement;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.rest.api.document.DocumentDto;
import com.neverpile.eureka.rest.api.document.content.ContentElementDto;
import com.neverpile.eureka.rest.api.document.content.ContentElementFacet;
import com.neverpile.eureka.rest.configuration.ModelMapperConfiguration.DocumentModelMapperConfigurer;
import com.neverpile.eureka.rest.configuration.ModelMapperConfiguration.ModelMapperConfigurationApplier;


@Configuration
@Import({DocumentModelMapperConfigurer.class, ModelMapperConfigurationApplier.class})
public class ModelMapperConfiguration {
  public interface ModelMapperConfigurer {
    void configure(ModelMapper mapper);
  }

  @Component
  public static class DocumentModelMapperConfigurer implements ModelMapperConfigurer {
    private final Optional<ContentElementFacet> contentElementFacet;
    
    public DocumentModelMapperConfigurer(final Optional<ContentElementFacet> contentElementFacet) {
      this.contentElementFacet = contentElementFacet;
    }
    
    @Override
    public void configure(final ModelMapper m) {
      m.addConverter(new AbstractConverter<Document, DocumentDto>() {
        @Override
        protected DocumentDto convert(final Document source) {
          DocumentDto dto = new DocumentDto();
          docToDto(source, dto, m);
          return dto;
        }
      });
      m.addConverter(new AbstractConverter<DocumentPdo, DocumentDto>() {
        @Override
        protected DocumentDto convert(final DocumentPdo source) {
          DocumentDto dto = new DocumentDto();
          docToDto(source, dto, m);
          return dto;
        }
      });

      m.addConverter(new AbstractConverter<DocumentDto, Document>() {
        @Override
        protected Document convert(final DocumentDto source) {
          Document doc = new Document();
          dtoToDoc(source, doc, m);
          return doc;
        }
      });
      m.addConverter(new AbstractConverter<DocumentDto, DocumentPdo>() {
        @Override
        protected DocumentPdo convert(final DocumentDto source) {
          DocumentPdo doc = new DocumentPdo();
          dtoToDoc(source, doc, m);
          return doc;
        }
      });
    }

    private void dtoToDoc(final DocumentDto source, final Document doc, final ModelMapper m) {
      doc.setDocumentId(source.getDocumentId());
      doc.setVersionTimestamp(source.getVersionTimestamp());

      contentElementFacet.ifPresent(f -> {
        ArrayList<ContentElement> contentElements = new ArrayList<>();
        if (source.getFacets().get("contentElements") != null) {
          for (ContentElementDto dto : source.getFacetData(f).orElseGet(Collections::emptyList)) {
            contentElements.add(m.map(dto, ContentElement.class));
          }
        }
        doc.setContentElements(contentElements);
      });

      doc.setDateCreated((Instant) source.getFacets().get("dateCreated"));
      doc.setDateModified((Instant) source.getFacets().get("dateModified"));
    }

    private void docToDto(final Document source, final DocumentDto dto, final ModelMapper m) {
      dto.setDocumentId(source.getDocumentId());
      dto.setVersionTimestamp(source.getVersionTimestamp());

      ArrayList<ContentElementDto> contentElementDtos = new ArrayList<>();
      if (source.getContentElements() != null) {
        for (ContentElement ce : source.getContentElements()) {
          contentElementDtos.add(m.map(ce, ContentElementDto.class));
        }
      }
      dto.setFacet("contentElements", contentElementDtos);

      dto.setFacet("dateCreated", source.getDateCreated());
      dto.setFacet("dateModified", source.getDateModified());
    }
  }
  

  @Component
  public static class ModelMapperConfigurationApplier {
    @Autowired(required = false)
    List<ModelMapperConfigurer> configurers;
    
    @Autowired
    ModelMapper modelMapper;

    @PostConstruct
    public void configureModelMapper() {
      if (null != configurers)
        configurers.forEach(c -> c.configure(modelMapper));
    }
  }
  
  @Bean
  @ConditionalOnMissingBean(ModelMapper.class)
  public ModelMapper modelMapper() {
    return new ModelMapper();
  }

}
